package com.github.hanfeng21050.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
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
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
                    progressIndicator.setText(String.format("[%s] %s", name, "auth获取成功"));
                    Logger.info(String.format("[%s] %s", name, "auth获取成功"));

                    // 获取应用id
                    String applicationId = SeeRequest.getApplication(seeConfig, applicationName, auth);
                    progressIndicator.setText(String.format("[%s] %s %s", name, "获取获取应用id成功，applicationId:", applicationId));
                    Logger.info(String.format(String.format("[%s] %s %s", name, "获取获取应用id成功，applicationId:", applicationId)));

                    if (StringUtils.isNotBlank(applicationId)) {
                        // 获取新版see的配置
                        boolean flag = false;
                        try {
                            JSONObject config = SeeRequest.getConfigInfoNew(seeConfig, applicationId, auth);
                            progressIndicator.setText(String.format("[%s] %s", name, "获取项目配置信息成功"));
                            Logger.info(String.format("[%s] %s", name, "获取项目配置信息成功"));
                            // 保存配置
                            saveConfigToFileNew(progressIndicator, config);
                            flag = true;
                        } catch (Exception e) {
                            progressIndicator.setText(String.format("[%s] %s", name, "see新版本配置获取失败"));
                            Logger.info(String.format("[%s] %s", name, "see新版本配置获取失败"));
                        }

                        if (!flag) {
                            JSONObject config = SeeRequest.getConfigInfo(seeConfig, applicationId, auth);
                            progressIndicator.setText(String.format("[%s] %s", name, "获取项目配置信息成功"));
                            Logger.info(String.format("[%s] %s", name, "获取项目配置信息成功"));

                            // 保存配置
                            saveConfigToFile(progressIndicator, config);
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage(String.format("[%s] %s", name, "获取项目配置信息成功"), "信息");
                        });
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage(String.format("[%s] %s", name, "未获取到当前项目配置文件，请检查"), "提示");
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
            Pattern pattern = Pattern.compile("([^/]+)$");
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                String fileName = matcher.group(1);
                String content = (String) config.get("content");
                saveFile(fileName, content);
            }
        }
    }

    private void saveConfigToFileNew(@NotNull ProgressIndicator indicator, JSONObject jsonObject) {
        if (jsonObject != null) {
            String fileName = jsonObject.getJSONObject("data").getString("fileName");
            String path = jsonObject.getJSONObject("data").getString("path");

            VirtualFile[] modules = project.getBaseDir().getChildren();
            for (VirtualFile module : modules) {
                if (module.getPath().contains("deploy")) {
                    try {
                        String resourceDirPath = "src/main/resources"; // 根据项目结构适当修改路径
                        // 使用 LocalFileSystem 构建资源目录的绝对路径
                        VirtualFile resourceDirectory = LocalFileSystem.getInstance().findFileByPath(module.getPath() + "/" + resourceDirPath);

                        File file = null;
                        if (resourceDirectory != null) {
                            file = new File(resourceDirectory.getPath() + "/" + fileName);
                        }

                        // 如果文件存在，则删除它
                        if (file != null && file.exists()) {
                            boolean delete = file.delete();
                        }
                        try (CloseableHttpResponse response = HttpClientUtil.httpGetResponse(seeConfig.getAddress() + "/acm/" + path)) {
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode == 200) {
                                HttpEntity entity = response.getEntity();
                                if (entity != null) {
                                    try (InputStream inputStream = entity.getContent();
                                         FileOutputStream outputStream = new FileOutputStream(file)) {
                                        byte[] buffer = new byte[8192];
                                        int bytesRead;
                                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, bytesRead);
                                        }
                                    }
                                }
                                // 解压文件， 获取配置
                                extractFilesFromNestedZip(resourceDirectory.getPath() + "/" + fileName, resourceDirectory.getPath());
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void extractFilesFromNestedZip(String zipFilePath, String outputDir) throws IOException {
        String regex = "home/hundsun/server/[^/]*(/config|/cust-config)/";
        Pattern pattern = Pattern.compile(regex);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(".zip")) {
                    // Extract the nested zip file to a temporary location
                    File tempFile = File.createTempFile("tempZip", ".zip");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }

                    try (ZipInputStream nestedZis = new ZipInputStream(new FileInputStream(tempFile))) {
                        ZipEntry nestedZipEntry;
                        while ((nestedZipEntry = nestedZis.getNextEntry()) != null) {
                            String filePath = nestedZipEntry.getName();
                            Matcher matcher = pattern.matcher(filePath);
                            // 保存文件
                            if (matcher.find()) {
                                String fileName = filePath.replaceAll(regex, "");
                                // 判断是否在过滤列表内，如果在则跳过
                                List<EasyEnvConfig.ExcludedFile> excludedFiles = config.getExcludedFiles();
                                Optional<EasyEnvConfig.ExcludedFile> any = excludedFiles.stream().filter(excludedFile -> CommonValidateUtil.isFileNameMatch(fileName, excludedFile.getFileName())).findAny();
                                if (any.isPresent()) {
                                    continue;
                                }

                                File outputFile = new File(outputDir, fileName);
                                if (outputFile.exists()) {
                                    outputFile.delete();
                                }

                                new File(outputFile.getParent()).mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                    byte[] buffer = new byte[1024];
                                    int length;

                                    if (fileName.endsWith(".dat")) {
                                        while ((length = nestedZis.read(buffer)) > 0) {
                                            fos.write(buffer, 0, length);
                                            Logger.info(String.format("[%s] %s", project.getName(), outputFile.getCanonicalPath()));
                                        }
                                    } else {
                                        while ((length = nestedZis.read(buffer)) > 0) {
                                            String content = new String(buffer, 0, length);
                                            // 根据配置规则替换文本内容
                                            List<EasyEnvConfig.ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
                                            for (EasyEnvConfig.ConfigReplaceRule configReplaceRule : configReplaceRules) {
                                                if (CommonValidateUtil.isFileNameMatch(fileName, configReplaceRule.getFileName())) {
                                                    String regExpression = configReplaceRule.getRegExpression();
                                                    String replaceStr = configReplaceRule.getReplaceStr();

                                                    Pattern pattern1 = Pattern.compile(regExpression);
                                                    Matcher matcher1 = pattern1.matcher(content);
                                                    content = matcher1.replaceAll(replaceStr);
                                                }
                                            }
                                            fos.write(content.getBytes());
                                        }
                                        Logger.info(String.format("[%s] %s", project.getName(), outputFile.getCanonicalPath()));
                                    }


                                }
                            }
                            nestedZis.closeEntry();
                        }
                    }

                    // Delete the temporary file
                    tempFile.delete();
                }
                zis.closeEntry();
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
        List<EasyEnvConfig.ExcludedFile> excludedFiles = config.getExcludedFiles();
        for (EasyEnvConfig.ExcludedFile excludedFile : excludedFiles) {
            String excludeFileName = excludedFile.getFileName();
            if (CommonValidateUtil.isFileNameMatch(fileName, excludeFileName)) {
                return;
            }
        }

        // 根据配置规则替换文本内容
        List<EasyEnvConfig.ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
        for (EasyEnvConfig.ConfigReplaceRule configReplaceRule : configReplaceRules) {
            if (CommonValidateUtil.isFileNameMatch(fileName, configReplaceRule.getFileName())) {
                String regExpression = configReplaceRule.getRegExpression();
                String replaceStr = configReplaceRule.getReplaceStr();

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
                    Logger.info(String.format("[%s] %s", project.getName(), file.getCanonicalPath()));

                    // 刷新资源目录，以便在IDE中显示文件
                    VfsUtil.markDirtyAndRefresh(true, true, true, resourceDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }
}
