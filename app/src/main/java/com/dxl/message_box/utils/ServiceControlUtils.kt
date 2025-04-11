package com.dxl.message_box.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import com.dxl.message_box.service.NotificationListenerService

/**
 * 服务控制工具类
 * 用于直接控制通知监听服务的启动和停止
 */
object ServiceControlUtils {
    
    private const val TAG = "ServiceControlUtils"
    
    /**
     * 检查通知监听服务是否正在运行
     */
    fun isServiceRunning(context: Context): Boolean {
        // 使用NotificationServiceUtils检查权限是否已授予
        val hasPermission = NotificationServiceUtils.isNotificationServiceEnabled(context)
        if (!hasPermission) {
            return false
        }
        
        // 如果权限已授予，我们假设服务可能正在运行
        // 注意：在Android中，无法直接确定服务是否正在运行
        // 我们可以使用SharedPreferences来记录服务状态
        val prefs = context.getSharedPreferences("service_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_service_running", false)
    }
    
    /**
     * 启动通知监听服务
     */
    fun startService(context: Context) {
        // 首先检查是否有通知监听权限
        if (!NotificationServiceUtils.isNotificationServiceEnabled(context)) {
            Toast.makeText(context, "请先授予通知访问权限", Toast.LENGTH_SHORT).show()
            NotificationServiceUtils.openNotificationListenerSettings(context)
            return
        }
        
        try {
            // 创建启动服务的Intent
            val intent = Intent(context, NotificationListenerService::class.java)
            context.startService(intent)
            
            // 记录服务状态
            val prefs = context.getSharedPreferences("service_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_service_running", true).apply()
            
            Toast.makeText(context, "通知监听服务已启动", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "通知监听服务已启动")
        } catch (e: Exception) {
            Toast.makeText(context, "启动服务失败: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "启动服务失败", e)
        }
    }
    
    /**
     * 停止通知监听服务
     */
    fun stopService(context: Context) {
        try {
            // 创建停止服务的Intent
            val intent = Intent(context, NotificationListenerService::class.java)
            // 先发送一个广播通知服务解除前台状态
            val stopForegroundIntent = Intent("com.dxl.Message_Box.STOP_FOREGROUND")
            stopForegroundIntent.setPackage(context.packageName)
            context.sendBroadcast(stopForegroundIntent)
            
            // 延迟后再停止服务，确保前台服务状态已完全解除
            android.os.Handler().postDelayed({
                val stopped = context.stopService(intent)
                Log.d(TAG, "服务停止结果: $stopped")
            }, 500) // 增加到500毫秒延迟，确保有足够时间处理广播
            
            // 对于NotificationListenerService，需要特殊处理
            // 先禁用再启用服务组件，强制系统重新绑定服务
            val componentName = ComponentName(context, NotificationListenerService::class.java)
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            
            // 短暂延迟后重新启用组件
            android.os.Handler().postDelayed({
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                
                // 记录服务状态
                val prefs = context.getSharedPreferences("service_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_service_running", false).apply()
                
                Toast.makeText(context, "通知监听服务已停止", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "通知监听服务已停止")
            }, 500) // 500毫秒延迟
            
        } catch (e: Exception) {
            Toast.makeText(context, "停止服务失败: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "停止服务失败", e)
        }
    }
    
    /**
     * 切换通知监听服务状态
     */
    fun toggleService(context: Context) {
        if (isServiceRunning(context)) {
            stopService(context)
        } else {
            startService(context)
        }
    }
}