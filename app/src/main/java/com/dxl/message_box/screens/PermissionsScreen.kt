package com.dxl.message_box.screens

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dxl.message_box.ui.theme.消息盒子Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavHostController) {
    val context = LocalContext.current
    
    // 状态变量，用于跟踪权限状态
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasBatteryOptimizationExemption by remember { mutableStateOf(false) }
    
    // 检查权限函数
    fun checkPermissions() {
        // 检查通知权限
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上使用POST_NOTIFICATIONS权限
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            // 旧版本检查通知监听服务权限
            val enabledNotificationListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val componentName = ComponentName(context, "com.dxl.message_box.service.NotificationListenerService")
            enabledNotificationListeners?.contains(componentName.flattenToString()) == true
        }
        
        // 检查电池优化豁免权限
        val powerManager = context.getSystemService(PowerManager::class.java)
        hasBatteryOptimizationExemption = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
    
    // 初始检查权限
    DisposableEffect(Unit) {
        checkPermissions()
        onDispose { }
    }
    
    // 监听页面重新获得焦点时刷新权限状态
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current
    LaunchedEffect(backDispatcher) {
        val callback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {}
        }
        backDispatcher?.onBackPressedDispatcher?.addCallback(callback)
        callback.isEnabled = false
    }
    
    // 使用LaunchedEffect监听Activity的生命周期
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    
    // 使用remember记住最后一次生命周期事件
    var lifecycleEvent by remember { mutableStateOf(androidx.lifecycle.Lifecycle.Event.ON_CREATE) }
    
    // 使用DisposableEffect设置生命周期观察者
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                lifecycleEvent = event
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    
    // 使用LaunchedEffect响应生命周期事件变化
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            // 当Activity恢复时重新检查权限
            checkPermissions()
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("权限设置") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 通知使用权限
            Text(
                text = "通知使用权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前状态: ${if (hasNotificationPermission) "已授权" else "未授权"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // 跳转到通知使用权限设置页面
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Android 13及以上先请求POST_NOTIFICATIONS权限
                        try {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 如果直接跳转失败，尝试打开应用设置页面
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    } else {
                        // 旧版本跳转到通知监听服务设置
                        try {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往授权")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // 电池优化权限
            Text(
                text = "电池优化权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前状态: ${if (hasBatteryOptimizationExemption) "已豁免" else "未豁免"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // 跳转到电池优化设置页面
                    try {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // 如果直接跳转失败，尝试打开电池优化设置页面
                        try {
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            // 如果还是失败，可以考虑显示一个提示
                            e2.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往授权")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    消息盒子Theme {
        PermissionsScreen(rememberNavController())
    }
}