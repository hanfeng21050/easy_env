package com.github.hanfeng21050.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author  hanfeng32305
 * Date  2023/11/1 0:26
 */
public class MyPluginLoader {

    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();
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
        // 判断是否在过滤列表内，如果在则跳过
        SortedMap<String, String> excludedFileMap = config.getExcludedFileMap();
        for (Map.Entry<String, String> stringStringEntry : excludedFileMap.entrySet()) {
            String excludeFileName = stringStringEntry.getValue();
            if (CommonValidateUtil.isFileNameMatch(fileName, excludeFileName)) {
                return;
            }
        }

        // 根据配置规则替换文本内容
        Map<String, EasyEnvConfig.ConfigReplaceRule> configReplaceRuleMap = config.getConfigReplaceRuleMap();
        for (Map.Entry<String, EasyEnvConfig.ConfigReplaceRule> stringConfigReplaceRuleEntry : configReplaceRuleMap.entrySet()) {
            EasyEnvConfig.ConfigReplaceRule rule = stringConfigReplaceRuleEntry.getValue();
            if (CommonValidateUtil.isFileNameMatch(fileName, rule.getFileName())) {
                String regExpression = rule.getRegExpression();
                String replaceStr = rule.getReplaceStr();

                Pattern pattern = Pattern.compile(regExpression);
                Matcher matcher = pattern.matcher(content);
                content = matcher.replaceAll(replaceStr);
            }
        }

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
}
