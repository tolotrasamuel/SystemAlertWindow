package `in`.jvapps.system_alert_window.views

import `in`.jvapps.system_alert_window.utils.Commons
import `in`.jvapps.system_alert_window.utils.Constants
import `in`.jvapps.system_alert_window.utils.UiBuilder
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout

class RowView(private val context: Context, private val rowMap: Map<String, Any>) {
    val view: LinearLayout
        get() {
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val columnsMap = rowMap["columns"] as List<Map<String, Any>>?
            val padding = UiBuilder.getPadding(
                context, Commons.getMapFromObject(rowMap, "padding")
            )
            linearLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            if (columnsMap != null) {
                for (i in columnsMap.indices) {
                    val eachColumn = columnsMap[i]
                    val textView =
                        UiBuilder.getTextView(context, Commons.getMapFromObject(eachColumn, "text"))
                    linearLayout.addView(textView)
                }
            }
            return linearLayout
        }
}