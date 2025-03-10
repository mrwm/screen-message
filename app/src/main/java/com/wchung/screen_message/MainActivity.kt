package com.wchung.screen_message

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.setPadding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    /* Settings */
    private var scrollText : Boolean = false

    /* Main part of the app */
    private var message : EditText? = null
    private var screen : TextView? = null
    private var isEmptyText : Boolean = true
    private var isVisible : Boolean = true
    private var textLines: Int = 1
    private var dp16 by Delegates.notNull<Int>()
    private var dp24 by Delegates.notNull<Int>()

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources.displayMetrics.densityDpi.toFloat() /
                DisplayMetrics.DENSITY_DEFAULT)
    }

    //used to suppress lint warning about setOnTouchListener
    @SuppressLint("ClickableViewAccessibility")
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

        dp24 = convertDpToPixel(24f, this).toInt()
        dp16 = convertDpToPixel(16f, this).toInt()

        // Get the root view and create a transition.
        val rootView = findViewById<ViewGroup>(R.id.main)
        val autoTransition = AutoTransition()
        //autoTransition.setDuration(300) // Without the keyboard, animation gets annoying fast...

        // Grab the text from intents
        val messageText : String? = getStringFromIntent(intent)

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
        message!!.setPadding(dp16)
        message!!.gravity = View.TEXT_ALIGNMENT_CENTER
        message!!.textAlignment = View.TEXT_ALIGNMENT_CENTER
        message!!.isSingleLine = false
        message!!.minHeight = dp16*2
        message!!.hint = getString(R.string.enter_message_here)
        message!!.maxLines = 10 // don't cover the whole screen. We want to be able to click out
        message!!.setText(messageText)

        // Find the screen TextView to display the message
        screen = findViewById(R.id.screen)
        if (messageText != null) {
            setText(messageText)
        }

        // Find the settings container
        val settings = findViewById<LinearLayout>(R.id.settings_container)
        rootView.removeView(settings)

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
            rootView.addView(settings)
        }

        screen?.maxLines = textLines

        // Use maxlines to workaround the issue of words
        // being cut off in the middle with autotextsizing.
        message?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                setText(s.toString())
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
                    rootView.addView(settings)
                }
                if (!isEmptyText && isVisible) {
                    TransitionManager.beginDelayedTransition(rootView, autoTransition)
                    rootView.removeView(message)
                    rootView.removeView(settings)
                }
                isVisible = message?.parent != null
            }
        }

        screen?.setOnClickListener {
            if (!isVisible) {
                TransitionManager.beginDelayedTransition(rootView, autoTransition)
                rootView.addView(message)
                rootView.addView(settings)
                message?.requestFocus()
                //toggleKeyboard(message!!, true)
            }
            if (!isEmptyText && isVisible) {
                TransitionManager.beginDelayedTransition(rootView, autoTransition)
                rootView.removeView(settings)
                rootView.removeView(message)
            }
            if (isEmptyText) {
                message?.requestFocus()
                //toggleKeyboard(message!!, true)
            }
            isVisible = message?.parent != null
        }

        // Implement a bottom sheet for listing all the settings.
        // The bottom sheet should be able to be dragged up and down, and past the bottom of
        // the screen in order to be hidden... or to only show the drag handle.
        var bottomDY = 0f
        var maxYPosition by Delegates.notNull<Float>()
        var minYPosition by Delegates.notNull<Float>()

        settings.post {
            maxYPosition = rootView.height.toFloat() - settings.height.toFloat()
            minYPosition = rootView.height.toFloat() - dp24
            settings.y = maxYPosition

            settings.animate()
                .y(minYPosition)
                //.setDuration(1000)
                .start()
        }

        settings.setOnTouchListener(View.OnTouchListener { view, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    //bottomDX = view!!.x - event.rawX
                    bottomDY = view!!.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    //var displacement = event.rawX + bottomDX
                    var displacement = event.rawY + bottomDY
                    if (displacement < maxYPosition) {
                        displacement = maxYPosition
                    }
                    if (displacement > minYPosition) {
                        displacement = minYPosition
                    }
                    view!!.animate()
                        //.x(displacement)
                        .y(displacement)
                        .setDuration(0)
                        .start()
                    view.performClick()
                }
                else -> {
                    return@OnTouchListener false
                }
            }
            return@OnTouchListener true
            }
        )

        // Settings for scrolling the text
        val scrollCheck = findViewById<CheckBox>(R.id.scrollCheckbox)
        scrollText = scrollCheck.isChecked
        scrollCheck.setOnClickListener {
            scrollText = scrollCheck.isChecked
            setText(message?.text.toString())
        }
    }

    /* This is probably not a good idea... seems kinda overcomplicating
    private fun transformView(duration: Long = 0L, view: View, minYPosition: Float = 0f, alpha : Float = 1f, removeSelf: Boolean = true) {
        var yPos = minYPosition
        if (yPos == 0f) {
            yPos = view.y
        }
        view.animate()
            .y(yPos)
            .alpha(alpha)
            .setDuration(duration)
            .start()

        if (removeSelf) view.removeSelf()
    }
    private fun View?.removeSelf() {
        this ?: return
        val parentView = parent as? ViewGroup ?: return
        parentView.removeView(this)
    }
    */

        private fun toggleKeyboard(view: View, @Suppress("SameParameterValue") show: Boolean = false) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS)
            if (show) {
                inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }

    private fun setText(text: String) {
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
        if (scrollText) {
            /* So this works... but at a horrifically slow pace. It is also not possible to change
            the marquee speed either, as the app compat library does not support it.

            Other methods like using animation to animate and scroll text does not work:
            The text will be cropped.

            Using a scrollview also has weird issues.
            */
            screen?.isSingleLine = true
            screen?.ellipsize = TextUtils.TruncateAt.MARQUEE
            screen?.marqueeRepeatLimit = -1
            screen?.isSelected = true
        }
        else {
            screen?.isSingleLine = false
            screen?.ellipsize = null
            screen?.marqueeRepeatLimit = 0
            screen?.isSelected = false
        }
        // Attempted to implement this, but this also causes problems if the user
        // enters text with sequential whitespaces.
        //textLines = message?.text?.split(' ', '\n')?.size ?: 1

        screen?.text = text
        isEmptyText = text.isEmpty()
        screen?.maxLines = textLines


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


    // suppress lint warning about deprecated intent.getParcelableArrayListExtra
    @Suppress("DEPRECATION")
    private fun getStringFromIntent(intent: Intent): String? {
        val intentAction = intent.action
        val intentType = intent.type
        Log.i("getStringFromIntent", "intentAction: $intentAction")
        Log.i("getStringFromIntent", "intentType: $intentType")

        var intentText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.i("getStringFromIntent", "intentText: $intentText")
        // Return immediately if there's text from the intent, not from the included content
        if (intentText != null) {
            return intentText
        }

        // Handle content that came with the intent
        val extras = intent.extras ?: return null
        if (Intent.ACTION_SEND == intentAction) {
            val singleFile = extras.getString(Intent.EXTRA_STREAM)
            val contentResolver = contentResolver
            try {
                checkNotNull(singleFile)
                val inputStream = checkNotNull(contentResolver.openInputStream(singleFile.toUri()))
                // Returns the file size in bytes
                //Log.i("getStringFromIntent", "File Size: " + inputStream.available());
                val r = BufferedReader(InputStreamReader(inputStream))
                val total = StringBuilder()
                var line: String?
                while ((r.readLine().also { line = it }) != null) {
                    total.append(line).append('\n')
                }
                inputStream.close()
                intentText = total.toString()
                //Log.i("getStringFromIntent", "intentText: " + intentText);
                return intentText
            } catch (e: IOException) {
                // Handle exceptions
                Log.e("StreamProcessing", "Error accessing stream data", e)
            }
            Toast.makeText(this, "Unable to parse the data", Toast.LENGTH_LONG).show()
            Log.wtf("getStringFromIntent", "Intent.ACTION_SEND: how did you get here?")
            return null
        }
        else if (Intent.ACTION_SEND_MULTIPLE == intentAction) {
            val uris: ArrayList<Uri?> = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!
            Log.i("getStringFromIntent", "uris: $uris")
            Toast.makeText(
                this,
                getString(R.string.multi_share_not_supported), Toast.LENGTH_LONG
            ).show()
        }
        Log.e("getStringFromIntent", "You somehow reached the end...")
        return null
    }


}