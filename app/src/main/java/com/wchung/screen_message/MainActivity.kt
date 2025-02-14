package com.wchung.screen_message

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
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
        // Hide the status and navigation bars
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

        // Get the root view and create a transition.
        var rootView = findViewById<ViewGroup>(R.id.main)
        var autoTransition = AutoTransition()
        autoTransition.setDuration(500) // Set to 500ms to make the fade more noticeable.

        // Use maxlines to workaround the issue of words
        // being cut off in the middle with autotextsizing.
        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                var textLines = 1
                val textLength = message?.text?.length
                if (textLength != null && textLength > 5) {
                    textLines = textLength / 5
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
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    message?.visibility = View.VISIBLE
                } else {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
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