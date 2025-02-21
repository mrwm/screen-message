package com.wchung.screen_message

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.DisplayMetrics
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
    private var isEmptyText : Boolean = true
    private var isVisible : Boolean = true
    private var textLines: Int = 1

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
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            //val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            //view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            ViewCompat.onApplyWindowInsets(v, insets)
        }

        //val dp8 : Int = convertDpToPixel(8f, this).toInt()
        val dp32 : Int = convertDpToPixel(32f, this).toInt()

        // Get the root view and create a transition.
        var rootView = findViewById<ViewGroup>(R.id.main)
        var autoTransition = AutoTransition()
        autoTransition.setDuration(500) // Set to 500ms to make the fade more noticeable.

        // Create the message EditText for user input
        message = EditText(this)
        message?.id = View.generateViewId()
        message!!.background = AppCompatResources.getDrawable(this, R.drawable.message_background)
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        message!!.layoutParams = layoutParams
        message!!.setPadding(dp32/2)
        message!!.gravity = View.TEXT_ALIGNMENT_CENTER
        message!!.textAlignment = View.TEXT_ALIGNMENT_CENTER
        message!!.isSingleLine = false
        message!!.minHeight = dp32
        message!!.hint = getString(R.string.enter_message_here)
        message!!.maxLines = 10 // don't cover the whole screen. We want to be able to click out

        // Find the screen TextView to display the message
        screen = findViewById(R.id.screen)

        // Restore the saved state if screen rotates
        if (savedInstanceState != null) {
            // So uhhh... which method is more efficient in both space + time complexity?
            //message?.setText(savedInstanceState.getString("message"))
            message?.text = Editable.Factory.getInstance().newEditable(
                                savedInstanceState.getString("message"))
            screen?.text = savedInstanceState.getString("screen")
            isVisible = savedInstanceState.getBoolean("visibleState")
            isEmptyText = savedInstanceState.getBoolean("isEmptyText")
            textLines = savedInstanceState.getInt("textLines")
        }
        if (isVisible) {
            rootView.addView(message)
        }

        screen?.maxLines = textLines

        // Use maxlines to workaround the issue of words
        // being cut off in the middle with autotextsizing.
        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                // Old implementation. It kinda works, but kinda splits text in unpredictable ways
                val textLength : Int? = message?.text?.length
                val newlines : Int? = message?.text?.split('\n')?.size
                // 4 is an arbitrary number. Could be smaller for three letter words,
                // but users can use newline to work around it
                val divisor = 4
                textLines = if (textLength != null && textLength > divisor) {
                    textLength / divisor
                } else {
                    1
                }
                if (newlines != null) {
                    textLines += newlines - 1
                }
                // Attempted to implement this, but this also causes problems if the user
                // enters text with sequential whitespaces.
                //textLines = message?.text?.split(' ', '\n')?.size ?: 1

                screen?.text = s.toString()
                isEmptyText = s.isEmpty()
                screen?.maxLines = textLines
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        message?.onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
            //Log.d("message", "Focus: $hasFocus")
            if (!hasFocus) {
                toggleKeyboard(view, false)
                if (isEmptyText && !isVisible) {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    rootView.addView(message)
                }
                if (!isEmptyText && isVisible) {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    rootView.removeView(message)
                }
                isVisible = message?.parent != null
            }
        }

        screen?.setOnClickListener {
            TransitionManager.beginDelayedTransition(rootView, autoTransition)
            if (!isVisible) {
                rootView.addView(message)
                message?.requestFocus()
                toggleKeyboard(message!!, true)
            }
            if (!isEmptyText && isVisible) {
                rootView.removeView(message)
            }
            if (isEmptyText) {
                message?.requestFocus()
                toggleKeyboard(message!!, true)
            }
            isVisible = message?.parent != null
        }
    }

    private fun toggleKeyboard(view: View, show: Boolean = false) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken,
                                    InputMethodManager.HIDE_NOT_ALWAYS)
        if (show) {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }


    // Save text properties when the screen rotates
    override fun onSaveInstanceState(outState: Bundle) {
        val msg : String = message?.text.toString()
        val isVisible : Boolean = message?.parent != null

        val scrnTxt : String = screen?.text.toString()

        outState.putString("message", msg)
        outState.putBoolean("visibleState", isVisible)
        outState.putBoolean("isEmptyText", isEmptyText)

        outState.putString("screen", scrnTxt)
        outState.putInt("textLines", textLines)
        super.onSaveInstanceState(outState)
    }
}