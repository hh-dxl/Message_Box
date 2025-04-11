package com.dxl.message_box.components

// 导入Android应用信息相关类
import android.content.pm.ApplicationInfo
// 导入Compose UI相关组件
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// 导入Material图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
// 导入Material3组件
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
// 导入Compose运行时相关组件
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// 导入UI对齐和修饰符相关组件
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// 导入图像处理相关组件
import androidx.core.graphics.drawable.toBitmap

/**
 * 应用选择器组件
 * 
 * 该组件用于显示设备上已安装的应用列表，并允许用户选择一个应用。
 * 包含搜索功能，可以通过应用名称或包名进行筛选。
 * 
 * @param installedApps 已安装的应用列表，包含应用的基本信息
 * @param onAppSelected 当用户选择应用时的回调函数，参数为所选应用的信息
 * @param selectedPackageName 预先选择的应用包名，默认为空字符串
 */
@Composable
fun AppSelector(
    installedApps: List<ApplicationInfo>,
    onAppSelected: (ApplicationInfo) -> Unit,
    selectedPackageName: String = ""
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    
    // 初始化已选择的应用
    // 当installedApps或selectedPackageName发生变化时，此效应会重新执行
    LaunchedEffect(installedApps, selectedPackageName) {
        if (selectedPackageName.isNotEmpty() && installedApps.isNotEmpty()) {
            // 查找匹配的应用：根据传入的包名在已安装应用列表中查找对应的应用
            val matchedApp = installedApps.find { it.packageName == selectedPackageName }
            // 如果找到匹配的应用，则更新选中状态
            if (matchedApp != null) {
                selectedApp = matchedApp
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "选择应用",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索应用") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 应用列表
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // 根据搜索关键词过滤应用列表
            // 过滤条件：应用名称或包名包含搜索关键词（不区分大小写）
            val filteredApps = installedApps.filter { app ->
                // 获取应用名称
                val appName = context.packageManager.getApplicationLabel(app).toString()
                // 检查应用名称或包名是否包含搜索关键词
                appName.contains(searchQuery.text, ignoreCase = true) ||
                app.packageName.contains(searchQuery.text, ignoreCase = true)
            }
            
            LazyColumn {
                items(filteredApps) { app ->
                    val appName = context.packageManager.getApplicationLabel(app).toString()
                    val packageName = app.packageName
                    val isSelected = selectedApp == app
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedApp = app
                                onAppSelected(app)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 应用图标
                        val appIcon = context.packageManager.getApplicationIcon(app.packageName)
                        Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // 应用信息
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = appName, fontSize = 16.sp)
                            Text(text = packageName, fontSize = 12.sp)
                        }
                        
                        // 选择状态
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedApp = app
                                onAppSelected(app)
                            }
                        )
                    }
                }
            }
        }
    }
}