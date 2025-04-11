package com.dxl.message_box.navigation

// 导入Compose相关组件
import androidx.compose.runtime.Composable
// 导入导航相关组件
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// 导入应用的各个屏幕
import com.dxl.message_box.screens.HomeScreen
import com.dxl.message_box.screens.HttpConfigScreen
import com.dxl.message_box.screens.MqttConfigScreen
import com.dxl.message_box.screens.PermissionsScreen

/**
 * 定义应用导航路由
 * 
 * 包含应用中所有屏幕的路由常量，用于导航控制器进行页面跳转
 */
object NavRoutes {
    /** 首页路由 */
    const val HOME = "home"
    /** 权限页面路由 */
    const val PERMISSIONS = "permissions"
    /** HTTP配置页面路由 */
    const val HTTP_CONFIG = "http_config"
    /** MQTT配置页面路由 */
    const val MQTT_CONFIG = "mqtt_config"
}

/**
 * 应用导航组件
 * 
 * 定义整个应用的导航结构，设置各个页面之间的导航关系
 * 使用Jetpack Navigation组件实现页面间的导航
 */
@Composable
fun AppNavigation() {
    // 创建导航控制器，用于管理应用内的导航
    val navController = rememberNavController()
    
    // 设置导航宿主，定义导航图，指定首页为起始目的地
    NavHost(navController = navController, startDestination = NavRoutes.HOME) {
        // 首页路由配置
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        // 权限页面路由配置
        composable(NavRoutes.PERMISSIONS) {
            PermissionsScreen(navController = navController)
        }
        // HTTP配置页面路由配置
        // 支持可选的configId参数，用于编辑现有配置
        composable(
            route = "${NavRoutes.HTTP_CONFIG}?configId={configId}",
            arguments = listOf(navArgument("configId") { nullable = true })
        ) { backStackEntry ->
            // 从导航参数中获取configId
            val configId = backStackEntry.arguments?.getString("configId")
            // 显示HTTP配置页面，并传入导航控制器和配置ID
            HttpConfigScreen(navController = navController, configId = configId)
        }
        // MQTT配置页面路由配置
        // 支持可选的configId参数，用于编辑现有配置
        composable(
            route = "${NavRoutes.MQTT_CONFIG}?configId={configId}",
            arguments = listOf(navArgument("configId") { nullable = true })
        ) { backStackEntry ->
            // 从导航参数中获取configId
            val configId = backStackEntry.arguments?.getString("configId")
            // 显示MQTT配置页面，并传入导航控制器和配置ID
            MqttConfigScreen(navController = navController, configId = configId)
        }
    }
}