package com.dxl.message_box.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dxl.message_box.R
import com.dxl.message_box.model.ConfigModel
import com.dxl.message_box.model.ConfigType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 通知监听服务
 * 用于监听系统通知，并根据配置规则处理通知
 */
class NotificationListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListener"
    }
    
    /**
     * 将通知可见性值转换为可读字符串
     */
    private fun getVisibilityString(visibility: Int): String {
        return when (visibility) {
            Notification.VISIBILITY_PUBLIC -> "公开"
            Notification.VISIBILITY_PRIVATE -> "私密"
            Notification.VISIBILITY_SECRET -> "秘密"
            else -> "未知"
        }
    }
    
    private val NOTIFICATION_CHANNEL_ID = "message_box_service_channel"
    private val NOTIFICATION_ID = 1001
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // 广播接收器，用于接收停止前台服务的广播
    private val stopForegroundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.dxl.Message_Box.STOP_FOREGROUND") {
                Log.d(TAG, "收到停止前台服务的广播")
                // 停止前台服务
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // 对于Android 13及以上版本使用新的API
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    // 对于较旧版本使用已弃用的API，但添加注释说明
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                Log.d(TAG, "前台服务已停止")
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "通知监听服务已创建")
        // 创建通知渠道（Android 8.0及以上需要）
        createNotificationChannel()
        
        // 注册广播接收器
        val filter = IntentFilter("com.dxl.Message_Box.STOP_FOREGROUND")
        registerReceiver(stopForegroundReceiver, filter, RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "广播接收器已注册")
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "消息盒子服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持消息盒子服务运行"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createForegroundNotification(): Notification {
        // 创建PendingIntent，点击通知时打开应用
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("消息盒子")
            .setContentText("服务正在运行")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        // 停止前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 对于Android 13及以上版本使用新的API
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            // 对于较旧版本使用已弃用的API，但添加注释说明
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        // 注销广播接收器
        try {
            unregisterReceiver(stopForegroundReceiver)
            Log.d(TAG, "广播接收器已注销")
        } catch (e: Exception) {
            Log.e(TAG, "注销广播接收器失败: ${e.message}")
        }
        
        super.onDestroy()
        Log.d(TAG, "通知监听服务已销毁")
        
        // 确保服务状态被正确记录
        val prefs = applicationContext.getSharedPreferences("service_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_service_running", false).apply()
        Log.i(TAG, "服务状态已更新为已停止")
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "通知监听服务已连接")
        
        // 请求锁屏通知访问权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 对于Android 8.0及以上版本，确保能够接收锁屏通知
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // 请求所有通知，包括锁屏通知
                requestInterruptionFilter(NotificationListenerService.INTERRUPTION_FILTER_ALL)
                // 请求所有通知类型
                requestListenerHints(NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS)
                Log.d(TAG, "已请求锁屏通知访问权限")
            } catch (e: Exception) {
                Log.e(TAG, "请求锁屏通知访问权限失败: ${e.message}")
            }
        }
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "通知监听服务已断开连接")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        // 记录详细日志，帮助调试锁屏通知问题
        Log.d(TAG, "收到通知事件: onNotificationPosted 被触发")
        Log.d(TAG, "通知来源: ${sbn.packageName}, 通知ID: ${sbn.id}, 发布时间: ${sbn.postTime}")
        Log.d(TAG, "是否正在显示: ${sbn.isOngoing}, 通知标志: ${sbn.notification.flags}")
        
        // 获取通知信息
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras
        
        // 检查通知可见性，确保在锁屏状态下也能获取通知内容
        val visibility = notification.visibility
        Log.d(TAG, "通知可见性: $visibility (${getVisibilityString(visibility)})")
        
        // 提取通知标题和内容
        var title = extras.getCharSequence("android.title")?.toString() ?: ""
        var text = extras.getCharSequence("android.text")?.toString() ?: ""
        
        // 如果是锁屏状态下的通知，尝试获取完整内容
        if (visibility == Notification.VISIBILITY_PRIVATE || visibility == Notification.VISIBILITY_SECRET) {
            // 尝试从公开版本获取内容
            val publicVersion = notification.publicVersion
            if (publicVersion != null) {
                val publicExtras = publicVersion.extras
                // 如果公开版本有内容，则使用公开版本
                val publicTitle = publicExtras.getCharSequence("android.title")?.toString()
                val publicText = publicExtras.getCharSequence("android.text")?.toString()
                
                if (!publicTitle.isNullOrEmpty()) title = publicTitle
                if (!publicText.isNullOrEmpty()) text = publicText
            }
            
            // 尝试从各种通知样式中获取内容
            // 1. 大文本样式
            val bigText = extras.getCharSequence("android.bigText")?.toString()
            if (!bigText.isNullOrEmpty() && (text.isEmpty() || text == "内容已隐藏")) {
                text = bigText
            }
            
            // 2. 收件箱样式
            val textLines = extras.getCharSequenceArray("android.textLines")
            if (textLines != null && textLines.isNotEmpty() && (text.isEmpty() || text == "内容已隐藏")) {
                text = textLines.joinToString("\n") { it.toString() }
            }
            
            // 3. 消息样式
            val messages = extras.getParcelableArray("android.messages")
            if (messages != null && messages.isNotEmpty() && (text.isEmpty() || text == "内容已隐藏")) {
                try {
                    // 尝试提取最新的消息内容
                    val lastMessage = messages.last()
                    // 通过反射获取消息内容
                    val messageObj = lastMessage.javaClass.getDeclaredField("mObj").apply { isAccessible = true }.get(lastMessage)
                    if (messageObj != null) {
                        // 获取消息文本
                        val messageText = messageObj.javaClass.getDeclaredMethod("getText").apply { isAccessible = true }.invoke(messageObj)?.toString()
                        if (!messageText.isNullOrEmpty()) {
                            text = messageText
                            Log.d(TAG, "成功从消息样式中提取内容: $text")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析消息样式通知失败: ${e.message}")
                }
            }
            
            // 4. 尝试从键盘输入通知中获取内容
            if ((text.isEmpty() || text == "内容已隐藏") && packageName == "android") {
                val remoteInputHistory = extras.getCharSequenceArray("android.remoteInputHistory")
                if (remoteInputHistory != null && remoteInputHistory.isNotEmpty()) {
                    text = remoteInputHistory.joinToString("\n") { it.toString() }
                    Log.d(TAG, "从远程输入历史中提取内容: $text")
                }
            }
        }

        Log.d(TAG, "收到通知: $packageName, 标题: $title, 内容: $text")
        
        // 获取所有配置
        val configList = ConfigModel.getConfigList(applicationContext)
        if (configList.isEmpty()) {
            Log.d(TAG, "没有配置，跳过处理")
            return
        }
        
        // 遍历配置列表，查找匹配的配置
        for (config in configList) {
            // 检查包名是否匹配
            if (config.appPackageName != packageName) {
                continue
            }
            
            // 检查关键词过滤
            val filterKeywords = config.filterKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (filterKeywords.isNotEmpty()) {
                val contentText = "$title $text"
                val matchesKeyword = filterKeywords.any { keyword -> contentText.contains(keyword) }
                if (!matchesKeyword) {
                    Log.d(TAG, "通知内容不包含关键词，跳过处理")
                    continue
                }
            }
            
            Log.d(TAG, "找到匹配的配置: ${config.name}, 类型: ${config.type}")
            
            // 根据配置类型处理通知
            when (config.type) {
                ConfigType.HTTP -> sendHttpNotification(config, packageName, title, text)
                ConfigType.MQTT -> sendMqttNotification(config, packageName, title, text)
            }
        }
    }
    
    /**
     * 发送HTTP通知
     * 支持在URL中使用$title和$text变量替换为实际的通知标题和内容
     */
    private fun sendHttpNotification(config: ConfigModel, packageName: String, title: String, text: String) {
        try {
            // 处理URL中的变量替换
            var processedUrl = config.serverUrl
            Log.d(TAG, "原始URL: $processedUrl")
            
            // 使用转义字符处理$符号，确保正确识别变量
            if (processedUrl.contains("\$title")) {
                Log.d(TAG, "检测到\$title变量，准备替换")
                processedUrl = processedUrl.replace("\$title", URLEncoder.encode(title, "UTF-8"))
                Log.d(TAG, "替换\$title后: $processedUrl")
            }
            
            if (processedUrl.contains("\$text")) {
                Log.d(TAG, "检测到\$text变量，准备替换")
                processedUrl = processedUrl.replace("\$text", URLEncoder.encode(text, "UTF-8"))
                Log.d(TAG, "替换\$text后: $processedUrl")
            }
            
            Log.d(TAG, "处理后的URL: $processedUrl")
            
            // 创建空请求体的POST请求
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val requestBody = "".toRequestBody(mediaType)
            val request = Request.Builder()
                .url(processedUrl)
                .post(requestBody) // 使用POST请求，但不传递额外参数
                .build()
            
            Log.d(TAG, "发送HTTP请求: ${processedUrl}")
            
            // 异步发送请求
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "HTTP请求失败: ${e.message}")
                }
                
                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "HTTP请求成功: ${response.code}")
                    response.close()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "发送HTTP通知异常: ${e.message}")
        }
    }
    
    /**
     * 发送MQTT通知
     * 支持在消息模板中使用$title、$text等变量替换为实际的通知标题和内容
     */
    private fun sendMqttNotification(config: ConfigModel, packageName: String, title: String, text: String) {
        try {
            // 准备发送的消息内容
            val messageContent: String
            
            // 检查是否有自定义消息模板
            if (config.mqttMessageTemplate.isNotEmpty()) {
                // 处理模板中的变量替换
                var processedTemplate = config.mqttMessageTemplate
                Log.d(TAG, "原始消息模板: $processedTemplate")
                
                // 替换变量 - 使用正则表达式确保正确识别变量
                Log.d(TAG, "开始替换变量，原始模板: $processedTemplate")
                
                // 使用字符串前加反斜杠来转义$符号，确保正则表达式正确识别
                if (processedTemplate.contains("\$title")) {
                    processedTemplate = processedTemplate.replace("\$title", title)
                    Log.d(TAG, "替换\$title后: $processedTemplate")
                }
                
                if (processedTemplate.contains("\$text")) {
                    processedTemplate = processedTemplate.replace("\$text", text)
                    Log.d(TAG, "替换\$text后: $processedTemplate")
                }
                
                if (processedTemplate.contains("\$app_package")) {
                    processedTemplate = processedTemplate.replace("\$app_package", packageName)
                    Log.d(TAG, "替换\$app_package后: $processedTemplate")
                }
                
                if (processedTemplate.contains("\$app_name")) {
                    processedTemplate = processedTemplate.replace("\$app_name", config.appName)
                    Log.d(TAG, "替换\$app_name后: $processedTemplate")
                }
                
                if (processedTemplate.contains("\$time")) {
                    processedTemplate = processedTemplate.replace("\$time", System.currentTimeMillis().toString())
                    Log.d(TAG, "替换\$time后: $processedTemplate")
                }
                
                Log.d(TAG, "变量替换过程完成，使用简单字符串替换而非正则表达式")
                
                Log.d(TAG, "处理后的消息内容: $processedTemplate")
                messageContent = processedTemplate
            } else {
                // 使用默认的JSON格式
                val jsonObject = JSONObject().apply {
                    put("app_package", packageName)
                    put("app_name", config.appName)
                    put("title", title)
                    put("content", text)
                    put("time", System.currentTimeMillis())
                }
                messageContent = jsonObject.toString()
            }
            
            // 创建MQTT客户端
            val persistence = MemoryPersistence()
            val brokerUrl = "tcp://${config.brokerUrl}:${config.port}"
            Log.d(TAG, "MQTT服务器地址: $brokerUrl")
            
            // 验证客户端ID，如果为空或无效则生成一个随机ID
            var clientId = config.clientId
            if (clientId.isBlank()) {
                // 生成一个随机的客户端ID，格式为：MessageBox_随机数
                clientId = "MessageBox_${System.currentTimeMillis()}"
                Log.d(TAG, "客户端ID为空，已生成随机ID: $clientId")
            }
            
            // 验证MQTT配置是否有效
            if (config.brokerUrl.isBlank()) {
                Log.e(TAG, "MQTT服务器地址为空，无法连接")
                return
            }
            
            val client = MqttClient(brokerUrl, clientId, persistence)
            
            // 设置连接选项
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                if (config.username.isNotEmpty()) {
                    userName = config.username
                    password = config.password.toCharArray()
                    Log.d(TAG, "使用认证信息连接MQTT服务器，用户名: ${config.username}, 密码长度: ${config.password.length}")
                } else {
                    Log.d(TAG, "未设置MQTT认证信息，尝试匿名连接")
                }
            }
            
            try {
                // 连接到MQTT代理
                Log.d(TAG, "开始连接MQTT服务器...")
                client.connect(options)
                Log.d(TAG, "MQTT服务器连接成功")
                
                // 发布消息
                val message = MqttMessage(messageContent.toByteArray())
                message.qos = 1
                
                // 检查主题是否有效
                if (config.topic.isBlank()) {
                    Log.e(TAG, "MQTT主题为空，无法发布消息")
                    client.disconnect()
                    client.close()
                    return
                }
                
                Log.d(TAG, "发布消息到主题: ${config.topic}")
                client.publish(config.topic, message)
                
                // 断开连接
                client.disconnect()
                client.close()
                
                Log.d(TAG, "MQTT消息发送成功")
            } catch (e: Exception) {
                Log.e(TAG, "MQTT连接或发布失败: ${e.message}")
                e.printStackTrace()
                // 确保资源被释放
                try {
                    if (client.isConnected) {
                        client.disconnect()
                    }
                    client.close()
                } catch (closeEx: Exception) {
                    Log.e(TAG, "关闭MQTT客户端异常: ${closeEx.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "发送MQTT通知异常: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "通知已移除: ${sbn.packageName}")
    }
    
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "通知监听服务收到启动命令")
        // 启动前台服务    
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        Log.d(TAG, "前台服务已启动")
        return START_STICKY //super.onStartCommand(intent, flags, startId)
    }
}