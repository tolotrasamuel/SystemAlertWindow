package `in`.jvapps.system_alert_window.utils

import `in`.jvapps.system_alert_window.BubbleActivity
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*

class NotificationHelper private constructor(private val mContext: Context?) {
    private val isMinAndroidQ: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val isMinAndroidR: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun initNotificationManager() {
        if (notificationManager == null) {
            if (mContext == null) {
                Log.e(TAG, "Context is null. Can't show the System Alert Window")
                return
            }
            notificationManager = mContext.getSystemService(
                NotificationManager::class.java
            )
            setUpNotificationChannels()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setUpNotificationChannels() {
        if (notificationManager!!.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = CHANNEL_DESCRIPTION
            notificationManager!!.createNotificationChannel(notificationChannel)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private fun updateShortcuts(icon: Icon) {
        val categories: MutableSet<String> = LinkedHashSet()
        categories.add("com.example.android.bubbles.category.TEXT_SHARE_TARGET")
        val shortcutInfo = ShortcutInfo.Builder(mContext, BUBBLE_SHORTCUT_ID)
            .setLocusId(LocusId(BUBBLE_SHORTCUT_ID)) //.setActivity(new ComponentName(mContext, BubbleActivity.class))
            .setShortLabel(SHORTCUT_LABEL)
            .setIcon(icon)
            .setLongLived(true)
            .setCategories(categories)
            .setIntent(Intent(mContext, BubbleActivity::class.java).setAction(Intent.ACTION_VIEW))
            .setPerson(
                Person.Builder()
                    .setName(SHORTCUT_LABEL)
                    .setIcon(icon)
                    .build()
            )
            .build()
        val shortcutManager =
            mContext!!.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
        shortcutManager.pushDynamicShortcut(shortcutInfo)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun createBubbleMetadata(
        icon: Icon,
        intent: PendingIntent
    ): Notification.BubbleMetadata {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Notification.BubbleMetadata.Builder(intent, icon)
                .setDesiredHeight(250)
                .setAutoExpandBubble(true)
                .setSuppressNotification(true)
                .build()
        } else {
            Notification.BubbleMetadata.Builder()
                .setDesiredHeight(250)
                .setIcon(icon)
                .setIntent(intent)
                .setAutoExpandBubble(true)
                .setSuppressNotification(true)
                .build()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun showNotification(
        icon: Icon,
        notificationTitle: String?,
        notificationBody: String?,
        params: HashMap<String, Any>?
    ) {
        if (isMinAndroidR) updateShortcuts(icon)
        val user = Person.Builder().setName("You").build()
        val person = Person.Builder().setName(notificationTitle).setIcon(icon).build()
        val bubbleIntent = Intent(mContext, BubbleActivity::class.java)
        bubbleIntent.action = Intent.ACTION_VIEW
        bubbleIntent.putExtra(Constants.INTENT_EXTRA_PARAMS_MAP, params)
        val pendingIntent = PendingIntent.getActivity(
            mContext,
            REQUEST_BUBBLE,
            bubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val now = System.currentTimeMillis() - 100
        val builder = Notification.Builder(mContext, CHANNEL_ID)
            .setBubbleMetadata(createBubbleMetadata(icon, pendingIntent))
            .setContentTitle(notificationTitle)
            .setSmallIcon(icon)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setShortcutId(BUBBLE_SHORTCUT_ID)
            .setLocusId(LocusId(BUBBLE_SHORTCUT_ID))
            .addPerson(person)
            .setShowWhen(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext,
                    REQUEST_CONTENT,
                    bubbleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setStyle(
                Notification.MessagingStyle(user)
                    .addMessage(
                        Notification.MessagingStyle.Message(
                            notificationBody!!,
                            now,
                            person
                        )
                    )
                    .setGroupConversation(false)
            )
            .setWhen(now)
        if (isMinAndroidR) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Click the icon in the end ->",
                    null
                ).build()
            )
        }
        notificationManager!!.notify(BUBBLE_NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification() {
        notificationManager!!.cancel(BUBBLE_NOTIFICATION_ID)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun areBubblesAllowed(): Boolean {
        return if (isMinAndroidR) {
            val notificationChannel =
                notificationManager!!.getNotificationChannel(
                    CHANNEL_ID,
                    BUBBLE_SHORTCUT_ID
                )!!
            notificationManager!!.areBubblesAllowed() || notificationChannel.canBubble()
        } else {
            val devOptions = Settings.Secure.getInt(
                mContext!!.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            )
            if (devOptions == 1) {
                Log.d(TAG, "Android bubbles are enabled")
                true
            } else {
                Log.e(
                    TAG,
                    "System Alert Window will not work without enabling the android bubbles"
                )
                Toast.makeText(
                    mContext,
                    "Enable android bubbles in the developer options, for System Alert Window to work",
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "bubble_notification_channel"
        private const val CHANNEL_NAME = "Incoming notification"
        private const val CHANNEL_DESCRIPTION = "Incoming notification description"
        private const val SHORTCUT_LABEL = "Notification"
        private const val BUBBLE_NOTIFICATION_ID = 1237
        private const val BUBBLE_SHORTCUT_ID = "bubble_shortcut"
        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2
        private var notificationManager: NotificationManager? = null
        private const val TAG = "NotificationHelper"
        private var mInstance: NotificationHelper? = null
        fun getInstance(context: Context?): NotificationHelper? {
            if (mInstance == null) {
                mInstance = NotificationHelper(context)
            }
            return mInstance
        }
    }

    init {
        if (isMinAndroidQ) initNotificationManager()
    }
}