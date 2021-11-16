package `in`.jvapps.system_alert_window.views

import `in`.jvapps.system_alert_window.utils.Commons
import `in`.jvapps.system_alert_window.utils.Constants
import `in`.jvapps.system_alert_window.utils.UiBuilder
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout

class BodyView(private val context: Context, private val bodyMap: Map<String, Any>) {
    val view: LinearLayout
        get() {
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            val decoration = UiBuilder.getDecoration(
                context, bodyMap[Constants.KEY_DECORATION]
            )
            if (decoration != null) {
                val gd = UiBuilder.getGradientDrawable(decoration)
                linearLayout.background = gd
            } else {
                linearLayout.setBackgroundColor(Color.WHITE)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            Commons.setMargin(context, params, bodyMap)
            linearLayout.layoutParams = params
            val padding = UiBuilder.getPadding(
                context, bodyMap[Constants.KEY_PADDING]
            )
            linearLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            val rowsMap = bodyMap[Constants.KEY_ROWS] as List<Map<String, Any?>>?
            if (rowsMap != null) {
                for (i in rowsMap.indices) {
                    val row = rowsMap[i]
                    linearLayout.addView(createRow(row))
                }
            }
            return linearLayout
        }

    private fun createRow(rowMap: Map<String, Any?>): View {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        Commons.setMargin(context, params, rowMap)
        linearLayout.layoutParams = params
        linearLayout.gravity =
            Commons.getGravity(rowMap[Constants.KEY_GRAVITY] as String?, Gravity.START)
        val padding = UiBuilder.getPadding(
            context, rowMap[Constants.KEY_PADDING]
        )
        linearLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        val decoration = UiBuilder.getDecoration(
            context, rowMap[Constants.KEY_DECORATION]
        )
        if (decoration != null) {
            val gd = UiBuilder.getGradientDrawable(decoration)
            linearLayout.background = gd
        }
        val columnsMap = rowMap[Constants.KEY_COLUMNS] as List<Map<String, Any>>?
        if (columnsMap != null) {
            for (j in columnsMap.indices) {
                val column = columnsMap[j]
                linearLayout.addView(createColumn(column))
            }
        }
        return linearLayout
    }

    private fun createColumn(columnMap: Map<String, Any>): View {
        val columnLayout = LinearLayout(context)
        columnLayout.orientation = LinearLayout.HORIZONTAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        Commons.setMargin(context, params, columnMap)
        columnLayout.layoutParams = params
        val padding = UiBuilder.getPadding(
            context, columnMap[Constants.KEY_PADDING]
        )
        columnLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        val decoration = UiBuilder.getDecoration(
            context, columnMap[Constants.KEY_DECORATION]
        )
        if (decoration != null) {
            val gd = UiBuilder.getGradientDrawable(decoration)
            columnLayout.background = gd
        }
        val textView =
            UiBuilder.getTextView(context, Commons.getMapFromObject(columnMap, Constants.KEY_TEXT))
        columnLayout.addView(textView)
        return columnLayout
    }
}