package `in`.jvapps.system_alert_window

import `in`.jvapps.system_alert_window.utils.*
import `in`.jvapps.system_alert_window.views.BodyView
import `in`.jvapps.system_alert_window.views.FooterView
import `in`.jvapps.system_alert_window.views.HeaderView
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BubbleActivity : AppCompatActivity() {
    private var bubbleLayout: LinearLayout? = null
    private var paramsMap: HashMap<String, Any>? = null
    private var mContext: Context? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble)
        mContext = this
        bubbleLayout = findViewById(R.id.bubbleLayout)
        val intent = intent
        if (intent != null && intent.extras != null) {
            paramsMap =
                intent.getSerializableExtra(Constants.INTENT_EXTRA_PARAMS_MAP) as HashMap<String, Any>
            configureUI()
        }
    }

    fun configureUI() {
        val headersMap = Commons.getMapFromObject(
            paramsMap!!, Constants.KEY_HEADER
        )
        val bodyMap = Commons.getMapFromObject(
            paramsMap!!, Constants.KEY_BODY
        )
        val footerMap = Commons.getMapFromObject(
            paramsMap!!, Constants.KEY_FOOTER
        )
        val headerView: LinearLayout = HeaderView(mContext!!, headersMap!!).view
        val bodyView: LinearLayout = BodyView(mContext!!, bodyMap!!).view
        val footerView: LinearLayout = FooterView(mContext!!, footerMap!!).view
        bubbleLayout!!.setBackgroundColor(Color.WHITE)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        bubbleLayout!!.layoutParams = params
        bubbleLayout!!.addView(headerView)
        bubbleLayout!!.addView(bodyView)
        bubbleLayout!!.addView(footerView)
    }
}