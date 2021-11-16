package `in`.jvapps.system_alert_window

import `in`.jvapps.system_alert_window.services.WindowServiceNew
import `in`.jvapps.system_alert_window.utils.Commons
import `in`.jvapps.system_alert_window.utils.Constants
import `in`.jvapps.system_alert_window.utils.NotificationHelper
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.view.FlutterCallbackInformation
import io.flutter.view.FlutterMain
import io.flutter.view.FlutterNativeView
import io.flutter.view.FlutterRunArguments
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

class SystemAlertWindowPlugin private constructor(
    context: Context,
    activity: Activity?,
    newMethodChannel: MethodChannel
) : Activity(), MethodCallHandler {
    private val mContext: Context?
    val methodChannelOfInstance: MethodChannel

    init {
        mContext = context
        mActivity = activity
        SystemAlertWindowPlugin.methodChannel = newMethodChannel
        methodChannelOfInstance = newMethodChannel
        methodChannel!!.setMethodCallHandler(this)
    }

    companion object {
        private var mActivity: Activity? = null

        @SuppressLint("StaticFieldLeak")
        private var sBackgroundFlutterView: FlutterNativeView? = null
        private var sPluginRegistrantCallback: PluginRegistrantCallback? = null
        var sIsIsolateRunning = AtomicBoolean(false)
        var methodChannel: MethodChannel? = null
        var backgroundChannel: MethodChannel? = null
        var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1237
        private val notificationManager: NotificationManager? = null
        private const val TAG = "SystemAlertWindowPlugin"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), Constants.CHANNEL)
            channel.setMethodCallHandler(
                SystemAlertWindowPlugin(
                    registrar.context(),
                    registrar.activity(),
                    channel
                )
            )
        }

        @JvmStatic
        fun setPluginRegistrant(callback: PluginRegistrantCallback?) {
            sPluginRegistrantCallback = callback
        }

        fun startCallBackHandler(context: Context?) {
            val preferences =
                context!!.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0)
            val callBackHandle = preferences.getLong(Constants.CALLBACK_HANDLE_KEY, -1)
            Log.d(TAG, "onClickCallBackHandle $callBackHandle")
            if (callBackHandle == -1L) return
            FlutterMain.ensureInitializationComplete(context, null)
            val mAppBundlePath = FlutterMain.findAppBundlePath()
            val flutterCallback =
                FlutterCallbackInformation.lookupCallbackInformation(callBackHandle)
            if (sBackgroundFlutterView == null) {
                sBackgroundFlutterView = FlutterNativeView(
                    context, true
                )
                if (sIsIsolateRunning.get()) {
                    Log.i(
                        TAG,
                        "Did not start callBackHandle... isolate is already running"
                    )
                    return
                }
                if (sPluginRegistrantCallback == null) {
                    Log.i(
                        TAG,
                        "Unable to start callBackHandle... as plugin is not registered"
                    )
                    return
                }
                Log.i(TAG, "Starting callBackHandle...")
                val args = FlutterRunArguments()
                args.bundlePath = mAppBundlePath
                args.entrypoint = flutterCallback.callbackName
                args.libraryPath = flutterCallback.callbackLibraryPath
                sBackgroundFlutterView!!.runFromBundle(args)
                sPluginRegistrantCallback!!.registerWith(
                    sBackgroundFlutterView!!.pluginRegistry
                )
            }
            if (backgroundChannel == null) {
                backgroundChannel =
                    MethodChannel(sBackgroundFlutterView, Constants.BACKGROUND_CHANNEL)
            }
            sIsIsolateRunning.set(true)
        }

        fun invokeCallBack(context: Context, type: String?, params: HashMap<String, Any>?, result: MethodChannel.Result? = null) {
            val argumentsList: MutableList<Any?> = ArrayList()
            Log.v(TAG, "invoking callback for tag $params")
            val preferences =
                context.getSharedPreferences(Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0)

            argumentsList.clear()
            argumentsList.add(type)
            argumentsList.add(params)
            if (!sIsIsolateRunning.get()) {
                Log.e(TAG, "invokeCallBack failed, as isolate is not running")
                return
            }
            if (backgroundChannel == null) {
                Log.v(TAG, "Recreating the background channel as it is null")
                backgroundChannel =
                    MethodChannel(sBackgroundFlutterView, Constants.BACKGROUND_CHANNEL)
            }
            try {
                Log.v(TAG, "Invoking on method channel")
                val retries = intArrayOf(2)
                invokeCallBackToFlutter(
                    backgroundChannel,
                    "callBack",
                    argumentsList,
                    retries,
                    result
                )
                //backgroundChannel.invokeMethod("callBack", argumentsList);
            } catch (ex: Exception) {
                Log.e(TAG, "Exception in invoking callback $ex")
            }

        }

        private fun invokeCallBackToFlutter(
            backgroundChannel: MethodChannel?,
            method: String,
            arguments: List<Any?>,
            retries: IntArray,
            result: MethodChannel.Result?
        ) {
            Log.e(TAG, "backgroundChannel invoking method  $method with $arguments result $result")
            backgroundChannel!!.invokeMethod(method, arguments, object : MethodChannel.Result {
                override fun success(o: Any?) {
                    Log.i(TAG, "Invoke call back success")
                    result?.success(o)
                }

                override fun error(s: String, s1: String?, o: Any?) {
                    Log.e(TAG, "Error $s$s1")
                    result?.error(s, s1, o)

                }

                override fun notImplemented() {
                    //To fix the dart initialization delay.
                    if (retries[0] <= 0) {
                        Log.e(TAG, "Not Implemented method $method")
                        result?.notImplemented()
                        return
                    }
                    Log.d(
                        TAG,
                        "Not Implemented method $method. Trying again to check if it works"
                    )
                    invokeCallBackToFlutter(backgroundChannel, method, arguments, retries, result)
                    retries[0]--
                }
            })
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        var prefMode: String?
        val arguments: List<*>
        Log.d(TAG, "onMethodCall is System Alert Window ${call.method}")

        when (call.method) {
            "getPlatformVersion" -> result.success("Android " + Build.VERSION.RELEASE)
            "requestPermissions" -> {
                assert(call.arguments != null)
                arguments = call.arguments as List<*>
                prefMode = arguments[0] as String?
                if (prefMode == null) {
                    prefMode = "default"
                }
                if (askPermission(!isBubbleMode(prefMode))) {
                    result.success(true)
                } else {
                    result.success(false)
                }
            }
            "checkPermissions" -> {
                arguments = call.arguments as List<*>
                prefMode = arguments[0] as String?
                if (prefMode == null) {
                    prefMode = "default"
                }
                if (checkPermission(!isBubbleMode(prefMode))) {
                    result.success(true)
                } else {
                    result.success(false)
                }
            }
            "isIsolateRunning" -> {
                return result.success(sIsIsolateRunning.get())
            }
            "connectToRunningIsolate" -> {
                methodChannel = methodChannelOfInstance
                return result.success(sIsIsolateRunning.get())
            }
            "broadcastFromIsolate" -> {
                arguments = call.arguments as List<*>
                val payload = arguments[0]
                methodChannel!!.invokeMethod("isolateBroadcast", payload)
                return result.success(true)
            }
            "sendEventFromFlutterToIsolate" -> {
                arguments = call.arguments as List<*>
                val id = arguments[0] as String
                val payload = arguments[1] as HashMap<String, Any>
//                methodChannel!!.invokeMethod("isolateBroadcast", payload)
//                return result.success(true)
                // TODO this is not yet working
                SystemAlertWindowPlugin.Companion.invokeCallBack(
                    mContext!!,
                    id,
                    payload,
                    result
                )
            }
            "showSystemWindow" -> {
                assert(call.arguments != null)
                arguments = call.arguments as List<*>
                val title = arguments[0] as String
                val body = arguments[1] as String
                val params = arguments[2] as HashMap<String, Any>
                prefMode = arguments[3] as String?
                if (prefMode == null) {
                    prefMode = "default"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isBubbleMode(prefMode)) {
                    if (checkPermission(false)) {
                        Log.d(TAG, "Going to show Bubble")
                        showBubble(title, body, params)
                    } else {
                        Toast.makeText(mContext, "Please give enable bubbles", Toast.LENGTH_LONG)
                            .show()
                        result.success(false)
                    }
                } else {
                    if (checkPermission(true)) {
                        Log.d(TAG, "Going to show System Alert Window")
                        val i = Intent(mContext, WindowServiceNew::class.java)
                        i.putExtra(Constants.INTENT_EXTRA_PARAMS_MAP, params)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        i.putExtra(WindowServiceNew.Companion.INTENT_EXTRA_IS_UPDATE_WINDOW, false)
                        //WindowService.enqueueWork(mContext, i);
                        mContext!!.startService(i)
                    } else {
                        Toast.makeText(
                            mContext,
                            "Please give draw over other apps permission",
                            Toast.LENGTH_LONG
                        ).show()
                        result.success(false)
                    }
                }
                result.success(true)
            }
            "updateSystemWindow" -> {
                assert(call.arguments != null)
                val updateArguments = call.arguments as List<*>
                val updateTitle = updateArguments[0] as String
                val updateBody = updateArguments[1] as String
                val updateParams = updateArguments[2] as HashMap<String, Any>
                prefMode = updateArguments[3] as String?
                if (prefMode == null) {
                    prefMode = "default"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isBubbleMode(prefMode)) {
                    if (checkPermission(false)) {
                        Log.d(TAG, "Going to update Bubble")
                        NotificationHelper.Companion.getInstance(mContext)!!.dismissNotification()
                        showBubble(updateTitle, updateBody, updateParams)
                    } else {
                        Toast.makeText(mContext, "Please enable bubbles", Toast.LENGTH_LONG).show()
                        result.success(false)
                    }
                } else {
                    if (checkPermission(true)) {
                        Log.d(TAG, "Going to update System Alert Window")
                        val i = Intent(mContext, WindowServiceNew::class.java)
                        i.putExtra(Constants.INTENT_EXTRA_PARAMS_MAP, updateParams)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        i.putExtra(WindowServiceNew.Companion.INTENT_EXTRA_IS_UPDATE_WINDOW, true)
                        //WindowService.enqueueWork(mContext, i);
                        mContext!!.startService(i)
                    } else {
                        Toast.makeText(
                            mContext,
                            "Please give draw over other apps permission",
                            Toast.LENGTH_LONG
                        ).show()
                        result.success(false)
                    }
                }
                result.success(true)
            }
            "closeSystemWindow" -> {
                arguments = call.arguments as List<*>
                prefMode = arguments[0] as String?
                if (prefMode == null) {
                    prefMode = "default"
                }
                if (checkPermission(!isBubbleMode(prefMode))) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isBubbleMode(prefMode)) {
                        NotificationHelper.Companion.getInstance(mContext)!!.dismissNotification()
                    } else {
                        val i = Intent(mContext, WindowServiceNew::class.java)
                        i.putExtra(WindowServiceNew.Companion.INTENT_EXTRA_IS_CLOSE_WINDOW, true)
                        //WindowService.dequeueWork(mContext, i);
                        mContext!!.startService(i)
                    }
                    result.success(true)
                }
            }
            "startIsolate" -> try {
                val arguments = call.arguments as List<*>?
                if (arguments == null) {
                    Log.e(TAG, "Unable to register on click handler. Arguments are null")
                    result.success(false)
                    return
                }
                val mainFlutterIsolateEntryPoint = arguments[0].toString().toLong()
                val preferences = mContext!!.getSharedPreferences(
                    Constants.SHARED_PREF_SYSTEM_ALERT_WINDOW, 0
                )
                preferences.edit().putLong(Constants.CALLBACK_HANDLE_KEY, mainFlutterIsolateEntryPoint).apply()
                startCallBackHandler(mContext)
                result.success(true)

            } catch (ex: Exception) {
                Log.e(TAG, "Exception in registerOnClickHandler $ex ")
                ex.printStackTrace()
                result.success(false)
            }
            else -> result.notImplemented()
        }
    }

    private fun isBubbleMode(prefMode: String): Boolean {
        val isPreferOverlay = "overlay".equals(prefMode, ignoreCase = true)
        return Commons.isForceAndroidBubble(mContext) ||
                !isPreferOverlay && ("bubble".equals(
            prefMode,
            ignoreCase = true
        ) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(mContext)) {
                Log.e(
                    TAG,
                    "System Alert Window will not work without 'Can Draw Over Other Apps' permission"
                )
                Toast.makeText(
                    mContext,
                    "System Alert Window will not work without 'Can Draw Over Other Apps' permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun askPermission(isOverlay: Boolean): Boolean {
        if (!isOverlay && (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)) {
            return NotificationHelper.Companion.getInstance(mContext)!!.areBubblesAllowed()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mContext!!.packageName)
                )
                if (mActivity == null) {
                    if (mContext != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        mContext.startActivity(intent)
                        Toast.makeText(
                            mContext,
                            "Please grant, Can Draw Over Other Apps permission.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Can't detect the permission change, as the mActivity is null")
                    } else {
                        Log.e(TAG, "'Can Draw Over Other Apps' permission is not granted")
                        Toast.makeText(
                            mContext,
                            "Can Draw Over Other Apps permission is required. Please grant it from the app settings",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    mActivity!!.startActivityForResult(
                        intent,
                        ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE
                    )
                }
            } else {
                return true
            }
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun checkPermission(isOverlay: Boolean): Boolean {
        if (!isOverlay && (Commons.isForceAndroidBubble(mContext) || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)) {
            //return NotificationHelper.getInstance(mContext).areBubblesAllowed();
            return true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(mContext)
        }
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun showBubble(title: String, body: String, params: HashMap<String, Any>) {
        val icon = Icon.createWithResource(mContext, R.drawable.ic_notification)
        val notificationHelper: NotificationHelper =
            NotificationHelper.Companion.getInstance(mContext)!!
        notificationHelper.showNotification(icon, title, body, params)
    }

}