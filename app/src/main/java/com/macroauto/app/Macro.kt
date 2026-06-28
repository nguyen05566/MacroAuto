package com.macroauto.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One recorded action: a tap at (x,y) executed after [delayMs] from the previous step. */
data class MacroStep(val x: Float, val y: Float, val delayMs: Long)

object MacroStore {
    private const val PREF = "macro_store"
    private const val KEY = "macro_steps"

    fun save(ctx: Context, steps: List<MacroStep>) {
        val arr = JSONArray()
        steps.forEach {
            arr.put(JSONObject().put("x", it.x).put("y", it.y).put("delay", it.delayMs))
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun load(ctx: Context): MutableList<MacroStep> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<MacroStep>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(MacroStep(o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getLong("delay")))
        }
        return out
    }
}
