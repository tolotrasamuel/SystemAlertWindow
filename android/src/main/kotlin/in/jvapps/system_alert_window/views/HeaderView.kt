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

class HeaderView(private val context: Context, private val headerMap: Map<String, Any>) {
    val relativeView: RelativeLayout
        get() {
            val relativeLayout = RelativeLayout(context)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            val decoration = UiBuilder.getDecoration(
                context, headerMap[Constants.KEY_DECORATION]
            )
            if (decoration != null) {
                val gd = UiBuilder.getGradientDrawable(decoration)
                relativeLayout.background = gd
            } else {
                relativeLayout.setBackgroundColor(Color.WHITE)
            }
            val titleMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_TITLE
            )
            val subTitleMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_SUBTITLE
            )
            val buttonMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_BUTTON
            )
            val padding = UiBuilder.getPadding(
                context, headerMap[Constants.KEY_PADDING]
            )
            relativeLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            val isShowButton = buttonMap != null
            assert(titleMap != null)
            val textColumn = createTextColumn(titleMap, subTitleMap)
            if (isShowButton) {
                val buttonPosition = headerMap[Constants.KEY_BUTTON_POSITION] as String?
                val button = UiBuilder.getButtonView(context, buttonMap)
                if ("leading" == buttonPosition) {
                    relativeLayout.addView(button)
                    relativeLayout.addView(textColumn)
                } else {
                    relativeLayout.addView(textColumn)
                    relativeLayout.addView(button)
                }
            } else {
                relativeLayout.addView(textColumn)
            }
            return relativeLayout
        }

    //assert titleMap != null;
    val view: LinearLayout
        get() {
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.HORIZONTAL
            val decoration = UiBuilder.getDecoration(
                context, headerMap[Constants.KEY_DECORATION]
            )
            if (decoration != null) {
                val gd = UiBuilder.getGradientDrawable(decoration)
                linearLayout.background = gd
            } else {
                linearLayout.setBackgroundColor(Color.WHITE)
            }
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val titleMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_TITLE
            )
            val subTitleMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_SUBTITLE
            )
            val buttonMap = Commons.getMapFromObject(
                headerMap, Constants.KEY_BUTTON
            )
            val padding = UiBuilder.getPadding(
                context, headerMap[Constants.KEY_PADDING]
            )
            linearLayout.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            val isShowButton = buttonMap != null
            //assert titleMap != null;
            val textColumn = createTextColumn(titleMap, subTitleMap)
            if (isShowButton) {
                val buttonPosition = headerMap[Constants.KEY_BUTTON_POSITION] as String?
                val button = UiBuilder.getButtonView(context, buttonMap)
                if ("leading" == buttonPosition) {
                    linearLayout.addView(button)
                    if (textColumn != null) {
                        linearLayout.addView(textColumn)
                    }
                } else {
                    if (textColumn != null) {
                        val param = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1.0f
                        )
                        textColumn.layoutParams = param
                        linearLayout.addView(textColumn)
                    }
                    linearLayout.addView(button)
                }
            } else {
                linearLayout.addView(textColumn)
            }
            return linearLayout
        }

    private fun createTextColumn(
        titleMap: Map<String, Any?>?,
        subTitleMap: Map<String, Any?>?
    ): View? {
        val titleView = UiBuilder.getTextView(context, titleMap)
        if (subTitleMap != null) {
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.addView(titleView)
            linearLayout.addView(UiBuilder.getTextView(context, subTitleMap))
            return linearLayout
        }
        return titleView
    }
}