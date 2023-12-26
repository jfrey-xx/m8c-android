package io.maido.m8client

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.maido.m8client.M8Key.DOWN
import io.maido.m8client.M8Key.EDIT
import io.maido.m8client.M8Key.LEFT
import io.maido.m8client.M8Key.OPTION
import io.maido.m8client.M8Key.PLAY
import io.maido.m8client.M8Key.RIGHT
import io.maido.m8client.M8Key.SHIFT
import io.maido.m8client.M8Key.UP
import io.maido.m8client.settings.GeneralSettings
import org.libsdl.app.SDLActivity


class M8SDLActivity : SDLActivity() {

    companion object {
        private const val TAG = "M8SDLActivity"


        fun startM8SDLActivity(context: Context) {
            val sdlActivity = Intent(context, M8SDLActivity::class.java)
            context.startActivity(sdlActivity)
        }

    }


    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()
        connect()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        hintAudioDriver(generalPreferences.audioDriver)
        lockOrientation(generalPreferences.lockOrientation)
    }

    override fun onUnhandledMessage(command: Int, param: Any?): Boolean {
        if (command == 0x8001 && param is Int) {
            val r = param shr 16
            val g = (param shr 8) and 0xFF
            val b = param and 0xFF
            Log.d(TAG, "Background color changed to $r $g $b")
            val main = findViewById<ViewGroup>(R.id.main)
            main.setBackgroundColor(Color.rgb(r, g, b))
            return true
        }
        return super.onUnhandledMessage(command, param)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Orientation to portrait, show alternative buttons")
            findViewById<View>(R.id.leftButtonsAlt)?.visibility = View.VISIBLE
            findViewById<View>(R.id.rightButtonsAlt)?.visibility = View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "Orientation to portrait, hide alternative buttons")
            findViewById<View>(R.id.leftButtonsAlt)?.visibility = View.GONE
            findViewById<View>(R.id.rightButtonsAlt)?.visibility = View.GONE
        }
        Handler(Looper.getMainLooper()).postDelayed({
            M8TouchListener.resetScreen();
        }, 100)
        super.onConfigurationChanged(newConfig)
    }

    override fun setContentView(view: View) {
        val mainLayout = FrameLayout(this)
        val m8Layout = layoutInflater.inflate(R.layout.m8, mainLayout, true)
        val generalPreferences = GeneralSettings.getGeneralPreferences(this)
        if (generalPreferences.showButtons) {
            val buttons = layoutInflater.inflate(R.layout.buttons, mainLayout, false)
            setButtonListeners(buttons)
            mainLayout.addView(buttons)
        } else {
            setButtonListeners(m8Layout)
        }
        val screen = mainLayout.findViewById<ViewGroup>(R.id.screen)
        screen.addView(view)
        super.setContentView(mainLayout)
    }

    private fun setButtonListeners(buttons: View) {
        mapOf(
            R.id.up to UP,
            R.id.upAlt to UP,
            R.id.down to DOWN,
            R.id.downAlt to DOWN,
            R.id.left to LEFT,
            R.id.leftAlt to LEFT,
            R.id.right to RIGHT,
            R.id.rightAlt to RIGHT,
            R.id.play to PLAY,
            R.id.playAlt to PLAY,
            R.id.shift to SHIFT,
            R.id.shiftAlt to SHIFT,
            R.id.option to OPTION,
            R.id.optionAlt to OPTION,
            R.id.edit to EDIT,
            R.id.editAlt to EDIT,
        )
            .forEach { (id, key) -> setListener(buttons, id, key) }
    }

    private fun setListener(buttons: View, viewId: Int, key: M8Key) {
        buttons.findViewById<View>(viewId)?.setOnTouchListener(M8TouchListener(key))
    }

    private external fun connect()

    private external fun hintAudioDriver(audioDriver: String?)

    private external fun lockOrientation(lock: Boolean)

}