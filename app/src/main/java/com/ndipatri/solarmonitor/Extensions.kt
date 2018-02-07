package com.ndipatri.solarmonitor

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun View.show(): View {
    visibility = View.VISIBLE
    return this
}

// NJD TODO - need to figure otu how to combine with above (View.show)
fun TextView.show(): TextView {
    visibility = View.VISIBLE
    return this
}

fun EditText.show(): EditText {
    visibility = View.VISIBLE
    return this
}

fun View.gone(): View {
    visibility = View.GONE
    return this
}

fun View.hide(): View {
    visibility = View.INVISIBLE
    return this
}