package com.rdt.kmclient

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.NullPointerException

class ActMain : AppCompatActivity(), View.OnClickListener {

    lateinit var m_shared_pref: SharedPreferences
    lateinit var m_ip: String
    lateinit var m_sock: Socket
    lateinit var m_sender: Formatter
    var m_client: ExecutorService = Executors.newCachedThreadPool()

    //
    // LIFECYCLE
    //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        v_vol_up.setOnClickListener(this)
        v_vol_down.setOnClickListener(this)
        v_vol_mute.setOnClickListener(this)
        v_up.setOnClickListener(this)
        v_down.setOnClickListener(this)
        v_left.setOnClickListener(this)
        v_right.setOnClickListener(this)
        v_left_click.setOnClickListener(this)
        v_right_click.setOnClickListener(this)
        v_3.setOnClickListener(this)

        v_touch_pad.setOnTouchListener(MyTouch())
        v_keyboard.setOnKeyListener(MyKey())
        v_keyboard.addTextChangedListener(MyTextWatcher())

        m_shared_pref = PreferenceManager.getDefaultSharedPreferences(this)
        m_ip = m_shared_pref.getString("ip_addr", "").toString()
        m_client = Executors.newCachedThreadPool()

        run_client()
    }

    override fun onResume() {
        super.onResume()
        v_keyboard.clearFocus()
        v_touch_pad.requestFocus()
    }

    override fun onDestroy() {
        try {
            m_sender.format(MyConfig.KM_LEAVE)
            m_sender.flush()
        }
        catch (e: NullPointerException) {}
        try {
            m_sender.close()
            m_sock.close()
        }
        catch (e: IOException) {}
        super.onDestroy()
    }

    //
    // CALLBACK
    //
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.v_setting) {
            startActivity(Intent(this, ActSetting::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.v_vol_up   -> { send(MyConfig.KM_V_UP) }
            R.id.v_vol_down -> { send(MyConfig.KM_V_DOWN) }
            R.id.v_vol_mute -> { send(MyConfig.KM_V_MUTE) }
            R.id.v_up    -> { send(MyConfig.KM_K_UP) }
            R.id.v_down  -> { send(MyConfig.KM_K_DOWN) }
            R.id.v_left  -> { send(MyConfig.KM_K_LEFT) }
            R.id.v_right -> { send(MyConfig.KM_K_RIGHT) }
            R.id.v_left_click  -> { send(MyConfig.KM_M_L_CLICK) }
            R.id.v_right_click -> { send(MyConfig.KM_M_R_CLICK) }
            R.id.v_3 -> { send(MyConfig.KM_K_HAN_ENG) }
            else -> {}
        }
    }

    override fun onBackPressed() {
        finish()
    }

    //
    //
    //
    fun run_client() {
        m_client.execute {
            while (true) {
                try {
                    Thread.sleep(500)
                }
                catch (e: InterruptedException) {}
                try {
                    m_ip = m_shared_pref.getString("ip_addr", "").toString()
                    if (m_ip == "") {
                    }
                    Log.d("tag", String.format("----ip=%s----", m_ip))
                    m_sock = Socket(m_ip, MyConfig.KM_PORT)
                    m_sender = Formatter(m_sock.getOutputStream())
                    m_sender.flush()
                    try {
                        send(MyConfig.KM_JOIN)
                        runOnUiThread(Runnable {
                            Toast.makeText(baseContext, "connected", Toast.LENGTH_LONG).show()
                        })
                        break
                    }
                    catch (e: NullPointerException) {}
                }
                catch (e: IOException) {
                }
            }
        }
    }

    fun send(message: String) {
        m_client.execute {
            try {
                m_sender.format(message)
                m_sender.flush()
            } catch (e: NullPointerException) {
            }
        }
    }

    //
    // CLASS
    //
    inner class MyTouch : View.OnTouchListener {
        var dnx = 0.0f
        var dny = 0.0f
        var upx = 0.0f
        var upy = 0.0f
        var px = 0.0f
        var py = 0.0f
        var incr = 0

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    dnx = event.x
                    dny = event.y
                }

                MotionEvent.ACTION_UP -> {
                    upx = event.x
                    upy = event.y
                    incr = 0
                    val dx = dnx - upx
                    val dy = dny - upy
                    if (Math.abs(dx) > MyConfig.KM_MIN_DISTANCE) {
                        if (dx < 0) { /* left to right swipe */ }
                        if (dx > 0) { /* right to left swipe */ }
                    }
                    if (Math.abs(dy) > MyConfig.KM_MIN_DISTANCE) {
                        if (dy < 0) { /* top to bottom swipe */ }
                        if (dy > 0) { /* bottom to top swipe */ }
                    }
                    //send(MyConfig.KM_M_L_CLICK)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (incr == 0) {
                        px = event.x
                        py = event.y
                        incr += 1
                    } else if (incr == 1) {
                        val xy = String.format(
                            "%s,%s,",
                            ((event.x - px).toInt()).toString(),
                            ((event.y - py).toInt()).toString()
                        )
                        send(xy)
                        px = event.x
                        py = event.y
                    }
                }

                else -> {}
            }
            return true
        }
    }

    inner class MyKey : View.OnKeyListener {
        override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event?.action == KeyEvent.ACTION_DOWN) {
                send(MyConfig.KM_K_ENTER)
                v_keyboard.setText("") // ENTER clear the text
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
                onBackPressed()
                return true
            }
            return false
        }
    }

    inner class MyTextWatcher : TextWatcher {
        lateinit var prev: String
        lateinit var curr: String
        var prev_han = 0
        var prev_len = 0

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prev = s.toString()
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            curr = s.toString()

            var num = 0
            val len = curr.length - prev.length

            if (curr.length > 0) {
                num = curr[curr.length - 1].toInt()
            }

            // BACKSPACE
            if (/*prev.startsWith(curr) &&*/ len == -1) {
                send(MyConfig.KM_K_BS)
                prev_han = num
                return
            }

            // ENTER
            if (len < 0 || (len == 0 && curr.length == 0)) {
                prev_han = 0
                return
            }

            // a char added
            if (curr.startsWith(prev) && len == 1) {
                // english or hangul key code
                if (num < 0xAC00) {
                    val key = s?.subSequence(s.length - 1, s.length).toString()
                    send(MyConfig.KM_K_ADD + key)
                    prev_han = 0
                    return
                }
                else { // num >= 0xAC00
                    // len == 1
                }
            }

            // len == 0 --> the add and del of hangul syllables does not make a difference of length
            // hangul unicode
            val han = num
            if (len == 0 && han < prev_han) {
                send(MyConfig.KM_K_BS)
                prev_han = han
                return
            }
            prev_han = han

            // hangul syllables unicode (1st, 2nd, 3rd)
            val trd = (((han - 0xAC00) % 28))
            val snd = (((han - 0xAC00 - trd) / 28) % 21)
            val fst = (((han - 0xAC00 - trd) / 28) - snd) / 21

            if (trd != 0) {
                send(MyConfig.KM_K_ADD + MyConfig.KM_3RD[trd])
            } else {
                send(MyConfig.KM_K_ADD + MyConfig.KM_2ND[snd])
            }
        }
        override fun afterTextChanged(s: Editable?) {
        }
    }

}

/* EOF */