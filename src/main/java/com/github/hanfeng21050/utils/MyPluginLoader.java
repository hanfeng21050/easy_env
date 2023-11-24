package com.github.hanfeng21050.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author  hanfeng32305
 * Date  2023/11/1 0:26
 */
public class MyPluginLoader {
    private final SeeConfig seeConfig;
    private final Project project;

    public MyPluginLoader(Project project, SeeConfig seeConfig) {
        this.seeConfig = seeConfig;
        this.project = project;
    }

    /**
     * 启动阻塞加载进程
     */
    public void startBlockingLoadingProcess() {
        // 禁用UI，使用户不能进行其他操作
        ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(project).setAlternativeResolveEnabled(true));

        // 执行加载任务
        ProgressManager.getInstance().run(new Task.Modal(project, "加载中...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // 在这里执行需要加载的任务，同时更新进度
                // 获取当前项目的名称
                String name = project.getName();
                String applicationName = name + "-svr";
                try {
                    // 登录并获取 auth 信息
                    SeeRequest.login(seeConfig);

                    String auth = SeeRequest.getAuth(seeConfig);
                    progressIndicator.setText("auth获取成功, auth：" + auth);

                    // 获取应用id
                    String applicationId = SeeRequest.getApplication(seeConfig, applicationName, auth);
                    progressIndicator.setText(applicationName + "获取获取应用id成功，applicationId:" + applicationId);

                    if (StringUtils.isNotBlank(applicationId)) {
                        // 获取配置信息
                        JSONObject config = SeeRequest.getConfigInfo(seeConfig, applicationId, auth);
                        progressIndicator.setText(applicationName + "获取项目配置信息成功");

                        // 保存配置
                        saveConfigToFile(progressIndicator, config);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage("项目" + applicationName + "配置获取成功", "信息");
                        });
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage("未获取到当前项目" + applicationName + "的配置文件，请检查", "提示");
                        });
                    }
                } catch (Exception ex) {
                    String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog("连接失败，请检查。" + errMsg, "错误");
                    });
                    throw new RuntimeException(ex);
                }

            }

            @Override
            public void onFinished() {
                // 任务完成后，启用UI
                ApplicationManager.getApplication().invokeLater(() -> DumbService.getInstance(project).setAlternativeResolveEnabled(false));
            }
        });
    }

    // login
    private void login(@NotNull ProgressIndicator indicator) throws URISyntaxException, IOException {
        HttpClientUtil.clearCookie();

        indicator.setText("开始登录：" + seeConfig.getAddress());
        String str = HttpClientUtil.httpGet(seeConfig.getAddress() + "/cas/login?get-lt=true");
        String ltPattern = "\"lt\":\"(.*?)\"";
        String executionPattern = "\"execution\":\"(.*?)\"";

        Pattern ltRegex = Pattern.compile(ltPattern);
        Pattern executionRegex = Pattern.compile(executionPattern);

        Matcher ltMatcher = ltRegex.matcher(str);
        Matcher executionMatcher = executionRegex.matcher(str);
        if (ltMatcher.find() && executionMatcher.find()) {
            String ltValue = ltMatcher.group(1);
            String executionValue = executionMatcher.group(1);

            Map<String, String> map = new HashMap<>();
            map.put("username", seeConfig.getUsername());
            // 加密
            map.put("password", seeConfig.getPassword());
            map.put("execution", executionValue);
            map.put("lt", ltValue);
            map.put("submit", "LOGIN");
            map.put("_eventId", "submit");
            // 登录
            HttpClientUtil.httpPost(seeConfig.getAddress() + "/cas/login", map);

            // 页面跳转，获取cookie
            HttpClientUtil.httpGet(seeConfig.getAddress() + "/cas/login?service=http%3A%2F%2F10.20.36.109%3A8081%2Facm%2Fcloud.htm");
            indicator.setFraction(0.2);
            indicator.checkCanceled();
        }
    }

    private String getAuth(@NotNull ProgressIndicator indicator) throws URISyntaxException, IOException {
        indicator.setText("正在获取auth信息");
        // /acm/system/auth.json
        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/system/auth.json", new HashMap<>());
        String tokenPattern = "\"token\":\"(.*?)\"";
        Pattern tokenRegex = Pattern.compile(tokenPattern);
        Matcher matcher = tokenRegex.matcher(str);
        if (matcher.find()) {
            String token = matcher.group(1);
            indicator.setFraction(0.3);
            indicator.checkCanceled();
            return token;
        }
        return "";
    }

    private String getApplication(@NotNull ProgressIndicator indicator, String applicationName, String auth) throws IOException {
        // /acm/dssp/application/authority/query.json
        indicator.setText("获取应用id：" + applicationName);
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        body.put("pageNo", "1");
        body.put("pageSize", "1");
        body.put("name", applicationName);
        body.put("allowedUpgradeMark", "true");
        header.put("Authorization", "Bearer " + auth);

        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/dssp/application/authority/query.json", body, header);
        JSONObject parse = JSONObject.parse(str);
        String errorInfo = (String) parse.get("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("items");
            if (!jsonArray.isEmpty()) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                return (String) jsonObject.get("id");
            }
        } else {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog("error: " + errorInfo, "错误");
            });
            throw new RuntimeException(errorInfo);
        }
        return "";
    }

    private JSONObject getConfig(@NotNull ProgressIndicator indicator, String applicationId, String auth) throws IOException {
        indicator.setText("读取配置信息：" + applicationId);
        // /acm/dssp/config/getCompareConfig.json
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("applicationId", applicationId);
        String str = HttpClientUtil.httpPost(seeConfig.getAddress() + "/acm/dssp/config/getCompareConfig.json", body, header);
        indicator.setFraction(0.5);
        indicator.checkCanceled();
        indicator.setText("读取配置成功：" + applicationId);
        return JSONObject.parse(str);
    }

    /**
     * @param indicator
     * @param jsonObject
     */
    private void saveConfigToFile(@NotNull ProgressIndicator indicator, JSONObject jsonObject) {
        JSONArray array = mergeNonEmptyConfig(jsonObject);
        for (int i = 0; i < array.size(); i++) {
            JSONObject config = array.getJSONObject(i);
            // 获取文件名称
            String path = (String) config.get("path");
            if (path.contains(".properties") || path.contains(".dat") || path.contains(".pfx")) {
                Pattern pattern = Pattern.compile("([^/]+)$");
                Matcher matcher = pattern.matcher(path);
                if (matcher.find()) {
                    String fileName = matcher.group(1);
                    indicator.setText("正在保存配置：" + fileName);
                    String content = (String) config.get("content");
                    // 处理配置内容
                    content = removeOrReplace(fileName, content);
                    saveFile(fileName, content);
                }
            }
        }
    }


    /**
     * 获取所有的config配置文件
     *
     * @param jsonObject
     * @return
     */
    public JSONArray mergeNonEmptyConfig(JSONObject jsonObject) {
        JSONArray mergedConfigs = new JSONArray();

        if (jsonObject.containsKey("config")) {
            JSONArray configArray = jsonObject.getJSONArray("config");
            if (configArray != null && !configArray.isEmpty()) {
                mergedConfigs.addAll(configArray);
            }
        }

        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                JSONArray nestedNonEmptyConfigs = mergeNonEmptyConfig((JSONObject) value);
                if (!nestedNonEmptyConfigs.isEmpty()) {
                    mergedConfigs.addAll(nestedNonEmptyConfigs);
                }
            } else if (value instanceof JSONArray) {
                for (Object arrayElement : (JSONArray) value) {
                    if (arrayElement instanceof JSONObject) {
                        JSONArray nestedNonEmptyConfigs = mergeNonEmptyConfig((JSONObject) arrayElement);
                        if (!nestedNonEmptyConfigs.isEmpty()) {
                            mergedConfigs.addAll(nestedNonEmptyConfigs);
                        }
                    }
                }
            }
        }

        return mergedConfigs;
    }


    /**
     * 保存文件
     *
     * @param fileName
     * @param content
     */
    public void saveFile(String fileName, String content) {
        VirtualFile[] modules = project.getBaseDir().getChildren();
        for (VirtualFile module : modules) {
            if (module.getPath().contains("deploy")) {
                try {
                    String resourceDirPath = "src/main/resources"; // 根据项目结构适当修改路径
                    // 使用LocalFileSystem构建资源目录的绝对路径
                    VirtualFile resourceDirectory = LocalFileSystem.getInstance().findFileByPath(module.getPath() + "/" + resourceDirPath);

                    File file = new File(resourceDirectory.getPath() + "/" + fileName);
                    // 如果文件存在，则删除它
                    if (file.exists()) {
                        file.delete();
                    }
                    if (fileName.contains(".dat")) {
                        // 配置文件转换二进制保存
                        byte[] bytes = Base64.decodeBase64(content);
                        FileUtils.writeByteArrayToFile(file, bytes);
                    } else {
                        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
                    }

                    // 刷新资源目录，以便在IDE中显示文件
                    VfsUtil.markDirtyAndRefresh(true, true, true, resourceDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }

    /**
     * 配置文件特殊处理
     *
     * @param fileName
     * @param content
     * @return
     */
    public String removeOrReplace(String fileName, String content) {
        // log4j2.xml 移除掉kafka相关节点
//        if (fileName.equals("log4j2.xml")) {
//            Pattern pattern = Pattern.compile("<Kafka.*?</Kafka>", Pattern.DOTALL);
//            Matcher matcher = pattern.matcher(content);
//            content = matcher.replaceAll("");
//        }

        // application.properties
        if (fileName.equals("application.properties")) {
            // 不需要这个配置文件
            content = content.replaceAll("\\./cust-config/emergency\\.properties,", "");

            // 替换配置文件路径
            content = content.replaceAll("\\./config/", "classpath:");

            // 注释app.host
            content = content.replaceAll("(app\\.host=)", "# $1");
        }

        if (fileName.equals("middleware.properties")) {
            // 替换配置文件路径
            content = content.replaceAll("files://\\./config/", "classpath:");
            content = content.replaceAll("\\./config/", "classpath:");
        }

        return content;
    }
}
