package `in`.jvapps.system_alert_window.services

import `in`.jvapps.system_alert_window.R
import `in`.jvapps.system_alert_window.SystemAlertWindowPlugin
import `in`.jvapps.system_alert_window.models.Margin
import `in`.jvapps.system_alert_window.utils.*
import `in`.jvapps.system_alert_window.views.BodyView
import `in`.jvapps.system_alert_window.views.FooterView
import `in`.jvapps.system_alert_window.views.HeaderView
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import java.util.*

class WindowServiceNew : Service(), View.OnTouchListener {
    private var wm: WindowManager? = null
    private var windowGravity: String? = null
    private var windowWidth = 0
    private var windowHeight = 0
    private var windowMargin: Margin? = null
    private var windowView: LinearLayout? = null
    private var headerView: LinearLayout? = null
    private var bodyView: LinearLayout? = null
    private var footerView: LinearLayout? = null
    private var offsetX = 0f
    private var offsetY = 0f
    private var originalXPos = 0
    private var originalYPos = 0
    private var moving = false
    private var mContext: Context? = null
    override fun onCreate() {
        createNotificationChannel()
        val notificationIntent = Intent(this, SystemAlertWindowPlugin::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay window service is running")
            .setSmallIcon(R.drawable.ic_desktop_windows_black_24dp)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        if (null != intent && intent.extras != null) {
            val paramsMap =
                intent.getSerializableExtra(Constants.INTENT_EXTRA_PARAMS_MAP) as HashMap<String, Any>?
            mContext = this
            val isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
            if (!isCloseWindow) {
                assert(paramsMap != null)
                val isUpdateWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                if (wm != null && isUpdateWindow && windowView != null) {
                    updateWindow(paramsMap)
                } else {
                    createWindow(paramsMap)
                }
            } else {
                closeWindow(true)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )!!
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun setWindowManager() {
        if (wm == null) {
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
        }
    }

    private fun setWindowLayoutFromMap(paramsMap: HashMap<String, Any>) {
        val headersMap = Commons.getMapFromObject(paramsMap, Constants.KEY_HEADER)
        val bodyMap = Commons.getMapFromObject(paramsMap, Constants.KEY_BODY)
        val footerMap = Commons.getMapFromObject(paramsMap, Constants.KEY_FOOTER)
        windowMargin = UiBuilder.getMargin(mContext, paramsMap[Constants.KEY_MARGIN])
        windowGravity = paramsMap[Constants.KEY_GRAVITY] as String?
        windowWidth = NumberUtils.getInt(paramsMap[Constants.KEY_WIDTH])
        windowHeight = NumberUtils.getInt(paramsMap[Constants.KEY_HEIGHT])
        headerView = HeaderView(mContext!!, headersMap!!).view
        if (bodyMap != null) bodyView = BodyView(mContext!!, bodyMap).view
        if (footerMap != null) footerView = FooterView(mContext!!, footerMap).view
    }

    private val layoutParams: WindowManager.LayoutParams
        private get() {
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
            return params
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun setWindowView(params: WindowManager.LayoutParams, isCreate: Boolean) {
        val isEnableDraggable = true //params.width == WindowManager.LayoutParams.MATCH_PARENT;
        if (isCreate) {
            windowView = LinearLayout(mContext)
        }
        windowView!!.orientation = LinearLayout.VERTICAL
        windowView!!.setBackgroundColor(Color.WHITE)
        windowView!!.layoutParams = params
        windowView!!.removeAllViews()
        windowView!!.addView(headerView)
        if (bodyView != null) windowView!!.addView(bodyView)
        if (footerView != null) windowView!!.addView(footerView)
        if (isEnableDraggable) windowView!!.setOnTouchListener(this)
    }

    private fun createWindow(paramsMap: HashMap<String, Any>?) {
        closeWindow(false)
        setWindowManager()
        setWindowLayoutFromMap(paramsMap!!)
        val params = layoutParams
        setWindowView(params, true)
        wm!!.addView(windowView, params)
        // send Window created event
        _onWindowCreated()
    }

    private fun _onWindowCreated() {
        SystemAlertWindowPlugin.Companion.invokeCallBack(
            mContext!!,
            Constants.CALLBACK_TYPE_WINDOW_CREATED,
            null
        )
    }

    private fun updateWindow(paramsMap: HashMap<String, Any>?) {
        setWindowLayoutFromMap(paramsMap!!)
        val params = windowView!!.layoutParams as WindowManager.LayoutParams
        params.width =
            if (windowWidth == 0) WindowManager.LayoutParams.MATCH_PARENT else Commons.getPixelsFromDp(
                mContext!!, windowWidth
            )
        params.height =
            if (windowHeight == 0) WindowManager.LayoutParams.WRAP_CONTENT else Commons.getPixelsFromDp(
                mContext!!, windowHeight
            )
        setWindowView(params, false)
        wm!!.updateViewLayout(windowView, params)
    }

    private fun closeWindow(isEverythingDone: Boolean) {
        Log.i(TAG, "Closing the overlay window")
        try {
            if (wm != null) {
                if (windowView != null) {
                    wm!!.removeView(windowView)
                    windowView = null
                }
            }
            wm = null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "view not found")
        }
        if (isEverythingDone) {
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
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

    override fun onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Destroying the overlay window service")
        val notificationManager =
            (applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val TAG = WindowServiceNew::class.java.simpleName
        const val CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val INTENT_EXTRA_IS_UPDATE_WINDOW = "IsUpdateWindow"
        const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
    }
}