package com.wchung.screen_message

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var message : EditText? = null
    private var screen : TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        message = findViewById(R.id.message)
        screen = findViewById(R.id.screen)

        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                screen?.text = s.toString()
                // you can call or do what you want with your EditText here
                // yourEditText...
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        screen?.setOnFocusChangeListener { view, b ->
            Log.i("screen", "focus changed: $b + $view")

            if (b) {
                message?.requestFocus()
            }
        }
    }
}