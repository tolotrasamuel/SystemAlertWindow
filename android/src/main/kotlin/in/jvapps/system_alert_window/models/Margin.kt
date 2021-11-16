package `in`.jvapps.system_alert_window.models

import `in`.jvapps.system_alert_window.utils.Commons
import `in`.jvapps.system_alert_window.utils.NumberUtils
import android.content.Context

class Margin(left: Any?, top: Any?, right: Any?, bottom: Any?, context: Context) {
    val left: Int
    val top: Int
    val right: Int
    val bottom: Int

    init {
        this.left = Commons.getPixelsFromDp(context, NumberUtils.getInt(left))
        this.top = Commons.getPixelsFromDp(context, NumberUtils.getInt(top))
        this.right = Commons.getPixelsFromDp(context, NumberUtils.getInt(right))
        this.bottom = Commons.getPixelsFromDp(context, NumberUtils.getInt(bottom))
    }
}