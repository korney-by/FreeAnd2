package com.korneysoft.freeand2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlin.coroutines.CoroutineContext
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
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
                //TODO("Not yet implemented")
            }

            override fun onDone(utteranceId: String?) {
                //TODO("Not yet implemented")
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
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }
}