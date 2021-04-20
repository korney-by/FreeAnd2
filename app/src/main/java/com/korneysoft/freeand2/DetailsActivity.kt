package com.korneysoft.freeand2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        intent?.getStringExtra("Request")?.let{ request ->
            findViewById<TextView>(R.id.request).text=request

        }
    }
}