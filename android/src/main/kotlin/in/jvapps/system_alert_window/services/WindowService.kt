package `in`.jvapps.system_alert_window.services

import `in`.jvapps.system_alert_window.models.Margin
import `in`.jvapps.system_alert_window.services.WindowService
import `in`.jvapps.system_alert_window.utils.*
import `in`.jvapps.system_alert_window.views.BodyView
import `in`.jvapps.system_alert_window.views.FooterView
import `in`.jvapps.system_alert_window.views.HeaderView
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.JobIntentService
import androidx.core.view.ViewCompat
import java.util.*

class WindowService : JobIntentService(), View.OnTouchListener {
    private var oServiceHandler: Handler? = null
    private var windowGravity: String? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var windowMargin: Margin? = null
    private var headerView: LinearLayout? = null
    private var bodyView: LinearLayout? = null
    private var footerView: LinearLayout? = null
    private var mContext: Context? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0
    private var moving = false
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating Window Service")
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        oServiceHandler = Handler()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //startTheServiceProcess(intent);
        Log.d(TAG, "onStartCommand")
        oServiceHandler = Handler()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onStopCurrentWork(): Boolean {
        closeOverlayService()
        return super.onStopCurrentWork()
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "Starting the service process")
        startTheServiceProcess(intent)
    }

    private fun startTheServiceProcess(intent: Intent?) {
        mContext = this
        if (null != intent && intent.extras != null) {
            Log.i(TAG, "Intent extras are not null")
            val isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
            if (!isCloseWindow) {
                var isUpdateWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                if (windowView == null || !ViewCompat.isAttachedToWindow(
                        windowView!!
                    )
                ) {
                    isUpdateWindow = false
                }
                if (!isUpdateWindow) {
                    closeOverlayService()
                    wm = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                } else {
                    try {
                        wm!!.removeView(windowView)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Caught exception $ex")
                    }
                }
                val paramsMap =
                    (intent.getSerializableExtra(Constants.INTENT_EXTRA_PARAMS_MAP) as Map<String, Any>?)!!
                val headersMap = Commons.getMapFromObject(
                    paramsMap, Constants.KEY_HEADER
                )
                val bodyMap = Commons.getMapFromObject(
                    paramsMap, Constants.KEY_BODY
                )
                val footerMap = Commons.getMapFromObject(
                    paramsMap, Constants.KEY_FOOTER
                )
                windowMargin = UiBuilder.getMargin(mContext, paramsMap[Constants.KEY_MARGIN])
                windowGravity = paramsMap[Constants.KEY_GRAVITY] as String?
                windowWidth = NumberUtils.getInt(paramsMap[Constants.KEY_WIDTH])
                windowHeight = NumberUtils.getInt(paramsMap[Constants.KEY_HEIGHT])
                headerView = HeaderView(mContext!!, headersMap!!).view
                if (bodyMap != null) bodyView = BodyView(mContext!!, bodyMap).view
                if (footerMap != null) footerView = FooterView(mContext!!, footerMap).view
                if (wm != null) {
                    showWindow(isUpdateWindow)
                } else {
                    Log.e(TAG, "Unable to show the overlay window as the window manager is null")
                }
            } else {
                closeOverlayService()
                try {
                    Log.d(TAG, "Calling stopSelf")
                    stopSelf()
                    Log.d(TAG, "Stopped self")
                } catch (ex: Exception) {
                    Log.d(TAG, "Exception in stopping self")
                }
            }
        } else {
            Log.e(TAG, "Intent extras are null!")
        }
    }

    private fun showWindow(isUpdateWindow: Boolean) {
        if (isUpdateWindow) {
            Log.d(TAG, "Updating the window")
        } else {
            Log.d(TAG, "Creating the window")
        }
        val params: WindowManager.LayoutParams
        params = WindowManager.LayoutParams()
        params.width =
            if (windowWidth == 0) WindowManager.LayoutParams.MATCH_PARENT else Commons.getPixelsFromDp(
                mContext!!, windowWidth
            )
        params.height =
            if (windowHeight == 0) WindowManager.LayoutParams.WRAP_CONTENT else Commons.getPixelsFromDp(
                mContext!!, windowHeight
            )
        params.format = PixelFormat.TRANSLUCENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            params.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            params.type =
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT or WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            params.flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        params.gravity = Commons.getGravity(windowGravity, Gravity.TOP)
        val windowMargin = windowMargin!!
        val marginTop: Int = windowMargin.top
        val marginBottom: Int = windowMargin.bottom
        val marginLeft: Int = windowMargin.left
        val marginRight: Int = windowMargin.right
        params.x = Math.max(marginLeft, marginRight)
        params.y =
            if (params.gravity == Gravity.TOP) marginTop else if (params.gravity == Gravity.BOTTOM) marginBottom else Math.max(
                marginTop,
                marginBottom
            )
        /* params.horizontalMargin = Math.max(marginLeft, marginRight);
        params.verticalMargin = (params.gravity == Gravity.TOP) ? marginTop :
                (params.gravity == Gravity.BOTTOM) ? marginBottom : Math.max(marginTop, marginBottom);*/
        //windowView.setOnTouchListener(this);
        oServiceHandler!!.post {
            val contentParams = LinearLayout.LayoutParams(params.width, params.height)
            buildWindowView(contentParams, params.width == WindowManager.LayoutParams.MATCH_PARENT)
        }
        oServiceHandler!!.post {
            //WindowService.this.buildWindowView();
            wm!!.addView(windowView, params)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildWindowView(params: LinearLayout.LayoutParams, enableDraggable: Boolean) {
        windowView = LinearLayout(mContext)
        windowView!!.orientation = LinearLayout.VERTICAL
        windowView!!.setBackgroundColor(Color.WHITE)
        windowView!!.layoutParams = params
        windowView!!.removeAllViews()
        windowView!!.addView(headerView)
        if (bodyView != null) windowView!!.addView(bodyView)
        if (footerView != null) windowView!!.addView(footerView)
        if (enableDraggable) windowView!!.setOnTouchListener(this)
    }

    private fun closeOverlayService() {
        Log.d(TAG, "Ending the service process")
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm!!.removeView(windowView)
                }
            }
            wm = null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "view not found")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (null != wm) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX
                val y = event.rawY
                moving = false
                val location = IntArray(2)
                windowView!!.getLocationOnScreen(location)
                originalXPos = location[0]
                originalYPos = location[1]
                offsetX = originalXPos - x
                offsetY = originalYPos - y
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val x = event.rawX
                val y = event.rawY
                val params = windowView!!.layoutParams as WindowManager.LayoutParams
                val newX = (offsetX + x).toInt()
                val newY = (offsetY + y).toInt()
                if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                    return false
                }
                params.x = newX
                params.y = newY
                wm!!.updateViewLayout(windowView, params)
                moving = true
            } else if (event.action == MotionEvent.ACTION_UP) {
                return moving
            }
        }
        return false
    }

    companion object {
        private const val TAG = "WindowService"
        const val JOB_ID = 1
        private const val INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow"
        private const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
        private var wm: WindowManager? = null

        @SuppressLint("StaticFieldLeak")
        private var windowView: LinearLayout? = null
        fun enqueueWork(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Start work")
            intent.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
            enqueueWork(context!!, WindowService::class.java, JOB_ID, intent)
        }

        fun updateWindow(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Update window")
            intent.putExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, true)
            enqueueWork(context!!, WindowService::class.java, JOB_ID, intent)
        }

        fun dequeueWork(context: Context?, intent: Intent) {
            Log.d(TAG, "Received - Stop work")
            intent.putExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, true)
            enqueueWork(context!!, WindowService::class.java, JOB_ID, intent)
        }
    }
}