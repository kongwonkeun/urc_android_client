package com.rdt.kmclient

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class ActSetting : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        supportFragmentManager.beginTransaction().replace(R.id.v_frag, FragSetting()).commit()
    }

}

/* EOF */