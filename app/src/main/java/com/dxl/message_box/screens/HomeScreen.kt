package com.dxl.message_box.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dxl.message_box.components.ConfigItem
import com.dxl.message_box.model.ConfigModel
import com.dxl.message_box.model.ConfigType
import com.dxl.message_box.navigation.NavRoutes
import com.dxl.message_box.ui.theme.消息盒子Theme
import com.dxl.message_box.utils.ServiceControlUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    // 添加菜单展开状态变量
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("消息盒子") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Box {
                        // 添加按钮 - 点击展开菜单
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Filled.Add, contentDescription = "添加配置文件")
                        }
                        
                        // 悬浮菜单
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("HTTP配置") },
                                leadingIcon = { Icon(Icons.Filled.Http, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(NavRoutes.HTTP_CONFIG)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("MQTT配置") },
                                leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(NavRoutes.MQTT_CONFIG)
                                }
                            )
                        }
                    }
                    
                    IconButton(onClick = { navController.navigate(NavRoutes.PERMISSIONS) }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                    }
                }
            )
        },
        floatingActionButton = {
            val context = LocalContext.current
            var isServiceEnabled by remember { mutableStateOf(ServiceControlUtils.isServiceRunning(context)) }
            
            // 添加DisposableEffect来监听应用焦点变化并更新服务状态
            DisposableEffect(Unit) {
                // 初始检查服务状态
                isServiceEnabled = ServiceControlUtils.isServiceRunning(context)
                
                // 创建一个定时器，每秒检查一次服务状态
                val timer = android.os.Handler(android.os.Looper.getMainLooper())
                val runnable = object : Runnable {
                    override fun run() {
                        val currentStatus = ServiceControlUtils.isServiceRunning(context)
                        if (isServiceEnabled != currentStatus) {
                            isServiceEnabled = currentStatus
                        }
                        timer.postDelayed(this, 1000) // 每秒检查一次
                    }
                }
                
                // 启动定时器
                timer.post(runnable)
                
                // 当组件被销毁时，移除回调以避免内存泄漏
                onDispose {
                    timer.removeCallbacks(runnable)
                }
            }
            
            FloatingActionButton(onClick = { 
                ServiceControlUtils.toggleService(context)
                isServiceEnabled = ServiceControlUtils.isServiceRunning(context)
            }) {
                Icon(
                    imageVector = if (isServiceEnabled) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isServiceEnabled) "停止服务" else "启动服务",
                    tint = if (isServiceEnabled) Color.Red else Color.White
                )
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current
        var configList by remember { mutableStateOf<List<ConfigModel>>(emptyList()) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var configToDelete by remember { mutableStateOf<ConfigModel?>(null) }
        
        // 加载配置列表
        LaunchedEffect(Unit) {
            configList = ConfigModel.getConfigList(context)
        }
        
        // 删除确认对话框
        if (showDeleteDialog && configToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除配置 '${configToDelete?.name}' 吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            configToDelete?.let { config ->
                                ConfigModel.deleteConfig(context, config.id)
                                configList = ConfigModel.getConfigList(context)
                                Toast.makeText(context, "配置已删除", Toast.LENGTH_SHORT).show()
                            }
                            showDeleteDialog = false
                            configToDelete = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            configToDelete = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 主内容区 - 配置列表或空白状态
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (configList.isEmpty()) {
                // 空白状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无配置文件\n点击右上角 + 按钮添加",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 配置列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(configList) { config ->
                        ConfigItem(
                            config = config,
                            onEdit = { editConfig ->
                                // 根据配置类型跳转到对应的编辑界面
                                when (editConfig.type) {
                                    ConfigType.HTTP -> navController.navigate("${NavRoutes.HTTP_CONFIG}?configId=${editConfig.id}")
                                    ConfigType.MQTT -> navController.navigate("${NavRoutes.MQTT_CONFIG}?configId=${editConfig.id}")
                                }
                            },
                            onDelete = { deleteConfig ->
                                configToDelete = deleteConfig
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    消息盒子Theme {
        HomeScreen(rememberNavController())
    }
}