package `in`.jvapps.system_alert_window.views

import `in`.jvapps.system_alert_window.utils.Commons
import `in`.jvapps.system_alert_window.utils.Constants
import `in`.jvapps.system_alert_window.utils.UiBuilder
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import java.util.*

class FooterView(private val context: Context, private val footerMap: Map<String, Any>) {
    val view: LinearLayout
        get() {
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val footerPadding = UiBuilder.getPadding(
                context, footerMap[Constants.KEY_PADDING]
            )
            linearLayout.setPadding(
                footerPadding.left,
                footerPadding.top,
                footerPadding.right,
                footerPadding.bottom
            )
            linearLayout.layoutParams = params
            val decoration = UiBuilder.getDecoration(
                context, footerMap[Constants.KEY_DECORATION]
            )
            if (decoration != null) {
                val gd = UiBuilder.getGradientDrawable(decoration)
                linearLayout.background = gd
            }
            if (footerMap[Constants.KEY_IS_SHOW_FOOTER] as Boolean) {
                val textMap = Commons.getMapFromObject(
                    footerMap, Constants.KEY_TEXT
                )
                val buttonsMap = Commons.getMapListFromObject(
                    footerMap, Constants.KEY_BUTTONS_LIST
                )
                val textView = UiBuilder.getTextView(context, textMap)
                val buttonsView: MutableList<Button?> = ArrayList()
                for (buttonMap in buttonsMap!!) {
                    buttonsView.add(UiBuilder.getButtonView(context, buttonMap))
                }
                val buttonsPosition = footerMap[Constants.KEY_BUTTONS_LIST_POSITION] as String?
                if (textView != null) {
                    if (buttonsView.size > 0) {
                        if ("leading" == buttonsPosition) {
                            for (buttonView in buttonsView) {
                                linearLayout.addView(buttonView)
                            }
                            linearLayout.addView(textView)
                        } else {
                            val param = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1.0f
                            )
                            textView.layoutParams = param
                            linearLayout.addView(textView)
                            for (buttonView in buttonsView) {
                                linearLayout.addView(buttonView)
                            }
                        }
                    } else {
                        linearLayout.addView(textView)
                    }
                } else {
                    for (buttonView in buttonsView) {
                        linearLayout.addView(buttonView)
                    }
                    linearLayout.gravity = Commons.getGravity(buttonsPosition, Gravity.FILL)
                }
            }
            return linearLayout
        }
}