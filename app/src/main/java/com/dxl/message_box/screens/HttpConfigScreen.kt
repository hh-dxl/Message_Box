package com.dxl.message_box.screens

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dxl.message_box.components.AppSelector
import com.dxl.message_box.model.ConfigModel
import com.dxl.message_box.model.ConfigType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

/**
 * HTTP配置界面
 */
@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpConfigScreen(navController: NavHostController, configId: String? = null) {
    val context = LocalContext.current
    val localConfigId by remember { mutableStateOf(configId ?: "") }
    var serviceName by remember { mutableStateOf(TextFieldValue("")) }
    var serverUrl by remember { mutableStateOf(TextFieldValue("")) }
    var filterKeywords by remember { mutableStateOf(TextFieldValue("")) }
    var selectedPackageName by remember { mutableStateOf("") }
    var selectedAppName by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    val focusManager = LocalFocusManager.current
    // 加载已安装的应用列表和现有配置（如果有）
    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { app ->
                pm.getApplicationLabel(app).toString().lowercase()
            }
        installedApps = apps

        // 如果传入了configId，加载现有配置
        if (localConfigId.isNotEmpty()) {
            val existingConfig = ConfigModel.getConfigById(context, localConfigId)
            if (existingConfig != null && existingConfig.type == ConfigType.HTTP) {

                // 填充表单
                serviceName = TextFieldValue(existingConfig.name)
                serverUrl = TextFieldValue(existingConfig.serverUrl)
                filterKeywords = TextFieldValue(existingConfig.filterKeywords)
                selectedPackageName = existingConfig.appPackageName
                selectedAppName = existingConfig.appName
            }
        }
    }

    // 在组件级别创建滚动状态，避免重组时重新创建
    val scrollState = rememberScrollState()
    
    // 使用DisposableEffect确保在组件销毁时正确清理滚动状态
    DisposableEffect(Unit) {
        onDispose {
            // 清理滚动状态相关资源
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HTTP配置") },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        focusManager.clearFocus()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 服务名称输入框
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("配置名称") },
                    placeholder = { Text("例如: 我的HTTP服务") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 服务器URL输入框
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("请求URL") },
                    placeholder = {
                        Text("例如: https://example.com/api/webhook/\$text")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "\$title 获取通知的标题\n\$text 获取通知的内容",
                    style = MaterialTheme.typography.bodySmall, // 使用小号文字样式
                    color = MaterialTheme.colorScheme.onBackground, // 设置文字颜色
                    modifier = Modifier.padding(top = 8.dp) // 与输入框保持一点距离
                )

                Spacer(modifier = Modifier.height(8.dp))
                // 应用选择器
                AppSelector(
                    installedApps = installedApps,
                    onAppSelected = { appInfo ->
                        selectedPackageName = appInfo.packageName
                        selectedAppName = context.packageManager.getApplicationLabel(appInfo).toString()
                    },
                    selectedPackageName = selectedPackageName
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 过滤词输入框
                OutlinedTextField(
                    value = filterKeywords,
                    onValueChange = { filterKeywords = it },
                    label = { Text("过滤关键词") },
                    placeholder = { Text("多个关键词用逗号分隔，只转发包含关键词的内容，为空则不过滤。") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.weight(1f))

                // 保存按钮
                Button(
                    onClick = {
                        // 保存配置
                        if (serviceName.text.isNotEmpty() && serverUrl.text.isNotEmpty() && selectedPackageName.isNotEmpty()) {
                            // 创建或更新配置
                            val config = ConfigModel(
                                id = localConfigId.ifEmpty { System.currentTimeMillis().toString() },
                                name = serviceName.text,
                                type = ConfigType.HTTP,
                                appPackageName = selectedPackageName,
                                appName = selectedAppName,
                                filterKeywords = filterKeywords.text,
                                serverUrl = serverUrl.text
                            )

                            // 保存配置
                            ConfigModel.saveConfig(context, config)

                            // 显示保存成功的提示
                            Toast.makeText(context, "HTTP配置已保存", Toast.LENGTH_SHORT).show()

                            // 返回上一页
                            navController.navigateUp()
                        } else {
                            Toast.makeText(context, "请填写服务名称、服务器地址并选择应用", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            }
        }
    }