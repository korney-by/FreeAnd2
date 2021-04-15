package com.korneysoft.freeand2

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    lateinit var requestInput: TextView
    lateinit var waEngine: WAEngine
    val searches = mutableListOf<HashMap<String, String>>()

    lateinit var reserchesAdaptes: SimpleAdapter
    lateinit var textToSpeech: TextToSpeech
    lateinit var stopButton: FloatingActionButton
    val TTS_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        initTts()
        initViews()
        initWolframEngine()

    }

    fun initTts() {
        textToSpeech = TextToSpeech(this) { code ->
            if (code == TextToSpeech.SUCCESS) {
                Log.d("MainActivity", "TextToSpeech.SUCCESS")
            } else {
                Log.e("MainActivity", "Error: $code")
            }
        }

        textToSpeech.language = Locale.US
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                stopButton.post { stopButton.visibility = View.VISIBLE }
            }

            override fun onDone(utteranceId: String?) {
                stopButton.post { stopButton.visibility = View.GONE }
            }

            override fun onError(utteranceId: String?) {
                //TODO("Not yet implemented")
            }
        })
    }


    fun initViews() {
        requestInput = findViewById(R.id.request_input)

        reserchesAdaptes = SimpleAdapter(
            applicationContext,
            searches,
            R.layout.item_search,
            arrayOf("Request", "Response"),
            intArrayOf(R.id.request, R.id.response)
        )
        val searchesList = findViewById<ListView>(R.id.searches_list)
        searchesList.adapter = reserchesAdaptes
        searchesList.setOnItemClickListener { parent, view, position, id ->
            val request = searches[position]["Resquest"]
            val response = searches[position]["Response"]
            textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, request)
            stopButton.visibility = View.VISIBLE
        }

        stopButton = findViewById(R.id.stop_button)
        stopButton.setOnClickListener {
            textToSpeech.stop()
            stopButton.visibility = View.GONE
        }
    }

    fun initWolframEngine() {
        waEngine = WAEngine()
        waEngine.appID = "VQ45HR-JY64LP2AT9"


        //waEngine.appID = "DEMO"

        waEngine.addFormat("plaintext")
    }

    fun askWolfram(request: String) {
        Toast.makeText(this, "Let me think...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            // Асинхронно на второстепенном потоке
            val query = waEngine.createQuery()
            query.input = request
            val queryResult = waEngine.performQuery(query)
            val response = if (queryResult.isError) {
                queryResult.errorMessage
            } else if (!queryResult.isSuccess) {
                "Sorry, I don't understand, can yourephrase?"
            } else {
                val str = StringBuilder()
                for (pod in queryResult.pods) {
                    if (!pod.isError) {
                        for (subpod in pod.subpods) {
                            for (element in subpod.contents) {
                                if (element is WAPlainText) {
                                    str.append(element.text)
                                }
                            }
                        }
                    }
                }
                str.toString()
            }

            withContext(Dispatchers.Main) {
                // Выполниться на главном потоке

                searches.add(0, HashMap<String, String>().apply {
                    put("Request", request)
                    put("Response", response)
                })
                reserchesAdaptes.notifyDataSetChanged()
                textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, request)

//                android.widget.Toast.makeText(
//                    applicationContext,
//                    response,
//                    android.widget.Toast.LENGTH_SHORT
//                ).show()

            }

        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                Log.d("MainActivity", "action_search")
                val request = requestInput.text.toString()
                askWolfram(request)
                return true
            }
            R.id.action_voice -> {
                Log.d("MainActivity", "action_voice")

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something, please ")
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
                try {
                    startActivityForResult(intent, TTS_REQUEST_CODE)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Извини чувак,работать не буде!", Toast.LENGTH_SHORT)
                        .show()
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TTS_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.let { question ->
                requestInput.text = question
                askWolfram(question)

            }
        }
    }
}