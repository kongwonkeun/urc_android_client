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

        v_touch_pad.setOnTouchListener(MyTouch())
        v_keyboard.setOnKeyListener(MyKey())
        v_keyboard.addTextChangedListener(MyTextWatcher())

        m_shared_pref = PreferenceManager.getDefaultSharedPreferences(this)
        m_ip = m_shared_pref.getString("ip_addr", "").toString()
        m_client = Executors.newCachedThreadPool()

        //run_client()
        v_3.setOnClickListener(this)
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
            R.id.v_3 -> { run_client() }
            else -> {}
        }
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
                        Log.d("tag", "----iiii----")
                    }
                    Log.d("tag", String.format("---- ip = %s ----", m_ip))
                    //m_sock = Socket(m_ip, MyConfig.KM_PORT)
                    m_sock = Socket("192.168.0.9", 9999)
                    Log.d("tag", "----jjjj----")
                    m_sender = Formatter(m_sock.getOutputStream())
                    Log.d("tag", "----kkkk----")
                    m_sender.flush()
                    Log.d("tag", "----llll----")
                    try {
                        send(MyConfig.KM_JOIN)
                        Log.d("tag", "----mmmm----")
                        runOnUiThread(Runnable {
                            Toast.makeText(baseContext, "connected", Toast.LENGTH_LONG).show()
                        })
                        break
                    }
                    catch (e: NullPointerException) {}
                }
                catch (e: IOException) {
                    Log.d("tag", "----eeee----")
                    //Toast.makeText(this, "connection failed", Toast.LENGTH_LONG).show()
                    //finish()
                }
            }
        }
    }

    fun send(message: String) {
        try {
            m_sender.format(message)
            m_sender.flush()
        }
        catch (e: NullPointerException) {}
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
            }
            return true
        }
    }

    inner class MyTextWatcher : TextWatcher {
        lateinit var prev: String
        lateinit var curr: String

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prev = s.toString()
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            curr = s.toString()
            val action = MyUtil.detect_key_action(prev, curr)
            if (action == MyConfig.KM_TYPE_BACKSPACE) {
                send(MyConfig.KM_K_BS)
            }
            if (action == MyConfig.KM_TYPE_KEY) {
                val key = s?.subSequence(s.length - 1, s.length).toString()
                send(MyConfig.KM_K_ADD + key)
            }
        }
        override fun afterTextChanged(s: Editable?) {
        }
    }

}

/* EOF */