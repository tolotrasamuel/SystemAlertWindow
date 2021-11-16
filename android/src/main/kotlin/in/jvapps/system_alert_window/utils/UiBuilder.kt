package `in`.jvapps.system_alert_window.utils

import `in`.jvapps.system_alert_window.SystemAlertWindowPlugin
import `in`.jvapps.system_alert_window.models.Decoration
import `in`.jvapps.system_alert_window.models.Margin
import `in`.jvapps.system_alert_window.models.Padding
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object UiBuilder {
    fun getTextView(context: Context, textMap: Map<String, Any?>?): TextView? {
        if (textMap == null) return null
        val textView = TextView(context)
        textView.text =
            textMap[Constants.KEY_TEXT] as String?
        textView.setTypeface(
            textView.typeface, Commons.getFontWeight(
                textMap[Constants.KEY_FONT_WEIGHT] as String?, Typeface.NORMAL
            )
        )
        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            NumberUtils.getFloat(textMap[Constants.KEY_FONT_SIZE])
        )
        textView.setTextColor(NumberUtils.getInt(textMap[Constants.KEY_TEXT_COLOR]))
        val padding = getPadding(context, textMap[Constants.KEY_PADDING])
        textView.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        return textView
    }

    fun getPadding(context: Context, `object`: Any?): Padding {
        val paddingMap = `object` as Map<String?, Any>?
            ?: return Padding(0, 0, 0, 0, context)
        return Padding(
            paddingMap[Constants.KEY_LEFT],
            paddingMap[Constants.KEY_TOP],
            paddingMap[Constants.KEY_RIGHT],
            paddingMap[Constants.KEY_BOTTOM],
            context
        )
    }

    fun getMargin(context: Context?, `object`: Any?): Margin {
        val marginMap = `object` as Map<String?, Any>?
            ?: return Margin(0, 0, 0, 0, context!!)
        return Margin(
            marginMap[Constants.KEY_LEFT],
            marginMap[Constants.KEY_TOP],
            marginMap[Constants.KEY_RIGHT],
            marginMap[Constants.KEY_BOTTOM],
            context!!
        )
    }

    fun getDecoration(context: Context, `object`: Any?): Decoration? {
        val decorationMap = `object` as Map<String?, Any>?
            ?: return null
        return Decoration(
            decorationMap[Constants.KEY_START_COLOR], decorationMap[Constants.KEY_END_COLOR],
            decorationMap[Constants.KEY_BORDER_WIDTH], decorationMap[Constants.KEY_BORDER_RADIUS],
            decorationMap[Constants.KEY_BORDER_COLOR], context
        )
    }

    fun getButtonView(context: Context, buttonMap: Map<String, Any>?): Button? {
        if (buttonMap == null) return null
        val button = Button(context)
        val map =  Commons.getMapFromObject(buttonMap, Constants.KEY_TEXT)
        val buttonText =
            getTextView(context, map)!!
        button.text = buttonText.text
        val tag = buttonMap[Constants.KEY_TAG] as String
        button.tag = tag
        button.textSize = Commons.getSpFromPixels(context, buttonText.textSize)
        button.setTextColor(buttonText.textColors)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) button.elevation = 10f
        val params = LinearLayout.LayoutParams(
            Commons.getPixelsFromDp(context, buttonMap[Constants.KEY_WIDTH] as Int),
            Commons.getPixelsFromDp(context, buttonMap[Constants.KEY_HEIGHT] as Int),
            1.0f
        )
        val buttonMargin: Margin = getMargin(context, buttonMap[Constants.KEY_MARGIN])
        params.setMargins(
            buttonMargin.left,
            buttonMargin.top,
            buttonMargin.right,
            Math.min(buttonMargin.bottom, 4)
        )
        button.layoutParams = params
        val padding = getPadding(context, buttonMap[Constants.KEY_PADDING])
        button.setPadding(padding.left, padding.top, padding.right, padding.bottom)
        val decoration = getDecoration(context, buttonMap[Constants.KEY_DECORATION])
        if (decoration != null) {
            val gd = getGradientDrawable(decoration)
            button.background = gd
        }
        button.setOnClickListener { v: View? ->
            if (!SystemAlertWindowPlugin.Companion.sIsIsolateRunning.get()) {
                SystemAlertWindowPlugin.Companion.startCallBackHandler(context)
            }
            val hashMap = HashMap<String, Any>()
            hashMap["tag"] = tag
            SystemAlertWindowPlugin.Companion.invokeCallBack(
                context,
                Constants.CALLBACK_TYPE_ONCLICK,
                hashMap
            )
        }
        return button
    }

    fun getGradientDrawable(decoration: Decoration): GradientDrawable {
        val gd = GradientDrawable()
        if (decoration.isGradient) {
            val colors = intArrayOf(decoration.startColor, decoration.endColor)
            gd.colors = colors
            gd.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        } else {
            gd.setColor(decoration.startColor)
        }
        gd.cornerRadius = decoration.borderRadius
        gd.setStroke(decoration.borderWidth, decoration.borderColor)
        return gd
    }
}