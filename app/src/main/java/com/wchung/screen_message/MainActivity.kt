package com.wchung.screen_message

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.setPadding


class MainActivity : AppCompatActivity() {
    private var message : EditText? = null
    private var screen : TextView? = null
    private var hasText : Boolean = false

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() /
                DisplayMetrics.DENSITY_DEFAULT)
    }

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

        val dp8 :  Int = convertDpToPixel(8f, this).toInt()
        val dp32 :  Int = convertDpToPixel(32f, this).toInt()

        // Get the root view and create a transition.
        var rootView = findViewById<ViewGroup>(R.id.main)
        var autoTransition = AutoTransition()
        autoTransition.setDuration(500) // Set to 500ms to make the fade more noticeable.

        //message = findViewById(R.id.message)
        //Log.i("onCreate", "stringType: " + stringType);
        screen = findViewById(R.id.screen)

        message = EditText(this)
        message?.id = View.generateViewId()
        message!!.background = AppCompatResources.getDrawable(this, R.drawable.message_background)
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        message!!.layoutParams = layoutParams
        message!!.setPadding(dp8)
        message!!.gravity = View.TEXT_ALIGNMENT_CENTER
        message!!.textAlignment = View.TEXT_ALIGNMENT_CENTER
        message!!.isSingleLine = false
        message!!.minHeight = dp32;
        message!!.hint = getString(R.string.enter_message_here)

        rootView.addView(message)

        // Restore the saved state if screen rotates
        if (savedInstanceState != null) {
            val text = savedInstanceState.getString("message")
            message?.setText(text)
        }


        // Use maxlines to workaround the issue of words
        // being cut off in the middle with autotextsizing.
        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                var textLines = 1
                val textLength = message?.text?.length
                val newlines = message?.text?.split('\n')?.size
                if (textLength != null && textLength > 5) {
                    textLines = textLength / 5
                }
                if (newlines != null) {
                    textLines += newlines
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
                if (!hasText && message?.parent == null) {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    rootView.addView(message)
                    //message?.visibility = View.VISIBLE
                }
                if (hasText) {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    rootView.removeView(message)
                    //message?.visibility = View.GONE
                }
            }
        }

        screen?.setOnClickListener {
            if (message?.parent == null) {
                TransitionManager.beginDelayedTransition(rootView, autoTransition)
                rootView.addView(message)
                //message?.visibility = View.VISIBLE
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


    override fun onSaveInstanceState(outState: Bundle) {
        val text: String = message?.text.toString()
        outState.putString("message", text)
        super.onSaveInstanceState(outState)
    }
}