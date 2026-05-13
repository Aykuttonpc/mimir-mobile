package com.aykutcincik.mimir.call.webrtc.utils

import android.util.Log

/**
 * GetStream sample'ından port edilen kodlar `io.getstream.log.taggedLogger` kullanır.
 * Bu paket bağımlılığı eklemek yerine `android.util.Log` üstünde bir lazy wrapper —
 * by-tag construction + lambda lazy-evaluation (debug log string format'ı production'da
 * eval edilmez).
 */
class TaggedLogger(val tag: String) {
    fun v(msg: () -> String) { if (Log.isLoggable(tag, Log.VERBOSE)) Log.v(tag, msg()) }
    fun d(msg: () -> String) { if (Log.isLoggable(tag, Log.DEBUG))   Log.d(tag, msg()) }
    fun i(msg: () -> String) { if (Log.isLoggable(tag, Log.INFO))    Log.i(tag, msg()) }
    fun w(msg: () -> String) { if (Log.isLoggable(tag, Log.WARN))    Log.w(tag, msg()) }
    fun e(msg: () -> String) { if (Log.isLoggable(tag, Log.ERROR))   Log.e(tag, msg()) }
}

fun taggedLogger(tag: String): Lazy<TaggedLogger> = lazy { TaggedLogger(tag) }
