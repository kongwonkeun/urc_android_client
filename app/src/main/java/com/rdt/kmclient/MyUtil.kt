package com.rdt.kmclient

class MyUtil {

    companion object {

        fun detect_key_action(prev: String, curr: String): Int {
            if (curr == "") {
                return MyConfig.KM_TYPE_BACKSPACE
            }
            if (prev.startsWith(curr) && (prev.length - curr.length) == 1) {
                return MyConfig.KM_TYPE_BACKSPACE
            }
            if (curr.startsWith(prev) && (curr.length - prev.length) == 1) {
                return MyConfig.KM_TYPE_KEY
            }
            return MyConfig.KM_TYPE_NONE
        }

    }

}

/* EOF */