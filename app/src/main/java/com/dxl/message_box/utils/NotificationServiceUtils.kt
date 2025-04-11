package com.dxl.message_box.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

/**
 * 通知监听服务工具类
 */
object NotificationServiceUtils {
    
    /**
     * 检查通知监听服务是否已启用
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledNotificationListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        val componentName = ComponentName(context, "com.dxl.message_box.service.NotificationListenerService")
        return enabledNotificationListeners?.contains(componentName.flattenToString()) == true
    }
    
    /**
     * 打开通知监听服务设置页面
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开通知监听设置页面", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    /**
     * 切换通知监听服务状态
     * 注意：由于Android系统限制，应用无法直接启用/禁用通知监听服务，
     * 只能引导用户到系统设置页面进行操作
     */
    fun toggleNotificationListenerService(context: Context) {
        if (isNotificationServiceEnabled(context)) {
            // 如果服务已启用，显示提示
            Toast.makeText(context, "请在设置中关闭通知监听服务", Toast.LENGTH_SHORT).show()
        }
        // 无论服务是否启用，都打开设置页面
        openNotificationListenerSettings(context)
    }
}