package com.dxl.message_box.model

// 导入Android上下文和共享首选项
import android.content.Context
// 导入Gson用于JSON序列化和反序列化
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 配置类型枚举
 * 
 * 定义应用支持的配置类型，目前包括HTTP和MQTT两种类型
 */
enum class ConfigType {
    /** HTTP配置类型，用于HTTP请求转发 */
    HTTP,
    /** MQTT配置类型，用于MQTT消息发布 */
    MQTT
}

/**
 * 配置数据模型
 * 
 * 用于存储和管理应用的配置信息，包括通用配置和特定类型的配置参数
 * 支持HTTP和MQTT两种配置类型，每种类型有其特定的参数
 */
data class ConfigModel(
    /** 配置ID，使用时间戳生成，用于唯一标识一个配置 */
    val id: String = "",
    /** 服务名称，用户定义的配置名称 */
    val name: String = "",
    /** 配置类型，指定当前配置是HTTP还是MQTT类型 */
    val type: ConfigType = ConfigType.HTTP,
    /** 应用包名，指定要监听的应用 */
    val appPackageName: String = "",
    /** 应用名称，用于显示 */
    val appName: String = "",
    /** 过滤关键词，用于过滤通知内容 */
    val filterKeywords: String = "", 
    
    // HTTP特有配置
    /** HTTP服务器URL，用于HTTP类型配置 */
    val serverUrl: String = "",
    
    // MQTT特有配置
    /** MQTT代理服务器地址，用于MQTT类型配置 */
    val brokerUrl: String = "",
    /** MQTT服务器端口，默认为1883 */
    val port: String = "1883",
    /** MQTT客户端ID，用于标识连接 */
    val clientId: String = "",
    /** MQTT用户名，用于认证 */
    val username: String = "",
    /** MQTT密码，用于认证 */
    val password: String = "",
    /** MQTT主题，用于发布消息 */
    val topic: String = "",
    /** MQTT消息模板，用于自定义发送内容，支持$title、$text等变量 */
    val mqttMessageTemplate: String = ""
) {
    /**
     * 伴生对象，提供配置的存储和管理功能
     * 
     * 使用SharedPreferences存储配置数据，通过Gson进行JSON序列化和反序列化
     */
    companion object {
        /** SharedPreferences存储名称 */
        private const val PREFS_NAME = "config_store"
        /** 配置列表在SharedPreferences中的键名 */
        private const val CONFIG_LIST_KEY = "config_list"
        
        /**
         * 保存配置列表到SharedPreferences
         * 
         * @param context Android上下文，用于获取SharedPreferences
         * @param configList 要保存的配置列表
         */
        fun saveConfigList(context: Context, configList: List<ConfigModel>) {
            // 获取SharedPreferences实例
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // 创建Gson实例用于JSON序列化
            val gson = Gson()
            // 将配置列表转换为JSON字符串
            val json = gson.toJson(configList)
            // 保存JSON字符串到SharedPreferences
            prefs.edit().putString(CONFIG_LIST_KEY, json).apply()
        }
        
        /**
         * 从SharedPreferences获取配置列表
         * 
         * @param context Android上下文，用于获取SharedPreferences
         * @return 配置模型列表，如果没有保存过配置则返回空列表
         */
        fun getConfigList(context: Context): List<ConfigModel> {
            // 获取SharedPreferences实例
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // 获取JSON字符串
            val json = prefs.getString(CONFIG_LIST_KEY, "")
            // 如果JSON为空，返回空列表
            if (json.isNullOrEmpty()) {
                return emptyList()
            }
            
            // 创建Gson实例用于JSON反序列化
            val gson = Gson()
            // 创建类型标记，用于指定反序列化的目标类型
            val type = object : TypeToken<List<ConfigModel>>() {}.type
            // 将JSON字符串转换为配置列表并返回
            return gson.fromJson(json, type)
        }
        
        /**
         * 添加或更新配置
         * 
         * 如果配置ID已存在，则更新现有配置；否则添加为新配置
         * 
         * @param context Android上下文，用于获取SharedPreferences
         * @param config 要保存的配置模型
         */
        fun saveConfig(context: Context, config: ConfigModel) {
            // 获取现有配置列表
            val configList = getConfigList(context).toMutableList()
            
            // 查找是否已存在相同ID的配置
            val existingIndex = configList.indexOfFirst { it.id == config.id }
            
            if (existingIndex >= 0) {
                // 更新现有配置
                configList[existingIndex] = config
            } else {
                // 添加新配置
                configList.add(config)
            }
            
            // 保存更新后的配置列表
            saveConfigList(context, configList)
        }
        
        /**
         * 删除配置
         * 
         * 根据配置ID删除对应的配置
         * 
         * @param context Android上下文，用于获取SharedPreferences
         * @param configId 要删除的配置ID
         */
        fun deleteConfig(context: Context, configId: String) {
            // 获取现有配置列表
            val configList = getConfigList(context).toMutableList()
            // 移除指定ID的配置
            configList.removeAll { it.id == configId }
            // 保存更新后的配置列表
            saveConfigList(context, configList)
        }
        
        /**
         * 根据ID获取配置
         * 
         * @param context Android上下文，用于获取SharedPreferences
         * @param configId 要查找的配置ID
         * @return 找到的配置模型，如果未找到则返回null
         */
        fun getConfigById(context: Context, configId: String): ConfigModel? {
            // 在配置列表中查找指定ID的配置并返回
            return getConfigList(context).find { it.id == configId }
        }
    }
}