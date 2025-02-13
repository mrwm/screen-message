package com.wchung.screen_message

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


class MainActivity : AppCompatActivity() {
    private var message : EditText? = null
    private var screen : TextView? = null
    private var hasText : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Add a listener to update the behavior of the toggle fullscreen button when
        // the system bars are hidden or revealed.
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            ViewCompat.onApplyWindowInsets(view, windowInsets)
        }

        message = findViewById(R.id.message)
        screen = findViewById(R.id.screen)

        var textLines = 1
        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val textLength = message?.text?.length
                if (textLength != null && textLength > 10) {
                    textLines = textLength / 10
                }
                screen?.text = s.toString()
                hasText = s.isNotEmpty()
                screen?.maxLines = textLines
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        message?.onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
            Log.d("message", "Focus: $hasFocus")
            if (!hasFocus) {
                hideKeyboard(view)
                if (!hasText) {
                    message?.visibility = View.VISIBLE
                } else {
                    message?.visibility = View.GONE
                }
            }
        }

        screen?.setOnClickListener {
            if (message?.visibility == View.GONE) {
                message?.visibility = View.VISIBLE
                message?.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(message, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}