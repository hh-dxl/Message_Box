package com.dxl.message_box

// 导入Android基础组件
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// 导入Compose相关组件
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
// 导入应用导航和主题
import com.dxl.message_box.navigation.AppNavigation
import com.dxl.message_box.ui.theme.消息盒子Theme

/**
 * 应用程序主活动
 * 
 * 作为应用的入口点，负责初始化UI并设置应用的主题和导航
 */
class MainActivity : ComponentActivity() {
    /**
     * 活动创建时的回调函数
     * 
     * @param savedInstanceState 保存的实例状态，用于恢复活动状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启用边缘到边缘显示，提供更大的显示区域
        enableEdgeToEdge()
        // 设置Compose内容
        setContent {
            // 应用消息盒子主题
            消息盒子Theme {
                // 设置应用导航结构
                AppNavigation()
            }
        }
    }
}

/**
 * 应用预览函数
 * 
 * 用于在Android Studio设计视图中预览应用界面
 */
@Preview(showBackground = true)
@Composable
fun AppPreview() {
    消息盒子Theme {
        AppNavigation()
    }
}