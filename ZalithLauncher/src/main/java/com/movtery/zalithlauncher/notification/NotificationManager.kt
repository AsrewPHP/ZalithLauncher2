package com.movtery.zalithlauncher.notification

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationManager {
    /**
     * 初始化通知，初始化通知渠道（频道）
     */
    fun initManager(activity: Activity) {
        NotificationChannelData.entries.forEach { data ->
            createNotificationChannel(activity, data)
        }
    }

    /**
     * 尝试检查通知权限是否开启，安卓 13 以下可能没法 100% 确定
     */
    fun checkNotificationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            //对一些魔改系统可能有效，但不能100%确定
            //所以在安卓 13 以下，尽量还是以默认不能使用通知对待
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            //SDK 33 以上有统一规范，不过实在是遇到那种傻逼系统，也没办法了说是
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_DENIED
        }
    }

    private fun createNotificationChannel(activity: Activity, channelData: NotificationChannelData) {
        val manager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelData.channelId, activity.getString(channelData.channelName), channelData.level).apply {
            channelData.channelDescription?.let { desRes ->
                description = activity.getString(desRes)
            }
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 跳转到通知设置页，出现异常则仅跳转到详细设置页
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent()
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
            context.startActivity(intent)
        } catch (e: Exception) {
            //如果出现异常，跳转到应用的详细设置页面
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }
    }
}