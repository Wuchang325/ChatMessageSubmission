package com.wuchang325.cms;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatMessageSubmission extends JavaPlugin implements Listener {

    private String targetUrl;

    @Override
    public void onEnable() {
        // 检查Java版本
        if (!isJavaVersionSupported()) {
            Bukkit.getLogger().severe("==============================================");
            Bukkit.getLogger().severe("ChatMessageSubmission 插件需要Java 17或更高版本才能运行。");
            Bukkit.getLogger().severe("当前Java版本: " + getJavaVersion());
            Bukkit.getLogger().severe("请升级Java版本后重新启动服务器。");
            Bukkit.getLogger().severe("==============================================");
            // 禁用插件并终止服务器
            Bukkit.getPluginManager().disablePlugin(this);
            Bukkit.shutdown();
            return;
        }

        // 获取插件的数据文件夹
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            // 创建插件的数据文件夹
            if (dataFolder.mkdirs()) {
                getLogger().info("已创建插件数据文件夹: " + dataFolder.getPath());
            } else {
                getLogger().severe("无法创建插件数据文件夹: " + dataFolder.getPath());
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        // 定义配置文件路径
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            // 创建默认的config.yml
            try {
                if (configFile.createNewFile()) {
                    
                    // 写入默认配置
                    String defaultConfig = 
                        "target-url: \"http://your-target-url.com/submit\"\n";
                    java.nio.file.Files.write(configFile.toPath(), defaultConfig.getBytes());
                } else {
                    getLogger().severe("无法创建 config.yml 文件。");
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
            } catch (IOException e) {
                getLogger().severe("创建 config.yml 文件时出错: " + e.getMessage());
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        // 加载配置文件
        saveDefaultConfig(); // 这将复制默认配置（如果config.yml不存在）
        reloadConfig(); // 重新加载配置以确保最新

        // 读取配置项
        targetUrl = getConfig().getString("target-url");
        if (targetUrl == null || targetUrl.isEmpty()) {
            getLogger().severe("配置文件中缺少 'target-url' 或其值为空。请在 config.yml 中正确配置。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        // 输出插件启动信息
        getLogger().info("ChatMessageSubmission 插件已启动!");
    }

    @Override
    public void onDisable() {
        // 输出插件关闭信息
        getLogger().info("ChatMessageSubmission 插件已关闭!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = event.getMessage();
        // 异步发送HTTP请求
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 创建URL对象
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                // 构建JSON数据
                String jsonInputString = "{\"player\": \"" + playerName + "\", \"message\": \"" + message + "\"}";
                // 发送数据
                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);           
                }
                // 获取响应码
                int responseCode = conn.getResponseCode();
                if(responseCode == 200) {
                    // 请求成功
                    getLogger().info("消息已成功提交到 " + targetUrl);
                } else {
                    // 请求失败
                    getLogger().warning("消息提交失败,响应码: " + responseCode);
                }
            } catch (Exception e) {
                // 处理异常
                getLogger().severe("消息提交过程中出现错误: " + e.getMessage());
            }
        });
    }

    /**
     * 检查当前Java版本是否支持
     * @return 如果Java版本是17或更高，返回true；否则返回false
     */
    private boolean isJavaVersionSupported() {
        String version = getJavaVersion();
        if (version == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("(\\d+)\\.\\d+\\.\\d+");
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            int majorVersion = Integer.parseInt(matcher.group(1));
            return majorVersion >= 17;
        }
        return false;
    }

    /**
     * 获取Java版本
     * @return 当前Java版本字符串
     */
    private String getJavaVersion() {
        return System.getProperty("java.version");
    }
}