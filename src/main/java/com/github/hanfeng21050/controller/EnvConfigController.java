package com.github.hanfeng21050.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.github.hanfeng21050.utils.CommonValidateUtil;
import com.github.hanfeng21050.utils.HttpClientUtil;
import com.github.hanfeng21050.utils.Logger;
import com.github.hanfeng21050.utils.ServiceUtils;
import com.intellij.openapi.application.ApplicationManager;
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
public class EnvConfigController {

    private final EasyEnvConfig config = ServiceUtils.getService(EasyEnvConfigComponent.class).getState();
    private final SeeConfig seeConfig;
    private final Project project;

    public EnvConfigController(Project project, SeeConfig seeConfig) {
        this.seeConfig = seeConfig;
        this.project = project;
    }

    /**
     * 获取环境配置文件
     */
    public void getEnvConfig() {
        String name = project.getName();
        String applicationName = name + "-svr";
        try {
            // 登录并获取 auth 信息
            Logger.info(String.format("[%s] 开始获取auth信息...", name));
            SeeRequestController.login(seeConfig);

            boolean stackMode = false;
            String auth = SeeRequestController.getAuth(seeConfig);
            Logger.info(String.format("[%s] auth获取成功", name));

            Logger.info(String.format("[%s] 开始获取应用ID，应用名称: %s", name, applicationName));
            String applicationId = SeeRequestController.getApplication(seeConfig, applicationName, auth);
            Logger.info(String.format("[%s] 获取应用ID成功: %s", name, applicationId));

            if (StringUtils.isEmpty(applicationId)) {
                // 获取应用id
                Logger.info(String.format("[%s] 应用ID获取为空, 开始获取Stack应用ID，应用名称: %s", name, applicationName));
                applicationId = SeeRequestController.getStackApplication(seeConfig, applicationName, auth);
                Logger.info(String.format("[%s] 获取应用Stack成功: %s", name, applicationId));
                stackMode = true;
            }

            if (StringUtils.isNotBlank(applicationId)) {
                Logger.info(String.format("[%s] 尝试获取See配置...", name));
                JSONObject config = SeeRequestController.getConfigInfo(seeConfig, applicationId, auth);
                Logger.info(String.format("[%s] 旧版See配置获取成功", name));

                // 保存配置
                saveConfigToFile(config, applicationName);
            } else {
                Logger.warn(String.format("[%s] 未获取到应用ID", name));
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage(String.format("[%s] %s", name, "未获取到当前项目配置文件，请检查"), "提示");
                });
            }
        } catch (Exception ex) {
            String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            Logger.error(String.format("[%s] 配置加载失败: %s", name, errMsg));
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog("连接失败，请检查。" + errMsg, "错误");
            });
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param jsonObject
     */
    private void saveConfigToFile(JSONObject jsonObject, String applicationName) {
        JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("node");
        for (int i = 0; i < jsonArray.size(); i++) {
            String name = jsonArray.getJSONObject(i).getString("name");
            if (applicationName.equals(name)) {
                jsonObject = jsonArray.getJSONObject(i);
            }
        }

        Logger.info(String.format("[%s] 开始保存配置文件...", project.getName()));
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
        Logger.info(String.format("[%s] 配置文件保存完成", project.getName()));
    }

    /**
     * @param jsonObject
     * @param stackMode
     */
    private void saveConfigToFileNew(JSONObject jsonObject, boolean stackMode) {
        Logger.info(String.format("[%s] 开始保存新版配置文件...", project.getName()));
        if (jsonObject != null) {
            String fileName = jsonObject.getJSONObject("data").getString("fileName");
            String path = jsonObject.getJSONObject("data").getString("path");

            VirtualFile[] modules = project.getBaseDir().getChildren();
            for (VirtualFile module : modules) {
                if (module.getPath().contains("deploy")) {
                    try {
                        String resourceDirPath = "src/main/resources"; // 根据项目结构适当修改路径
                        Logger.info(String.format("[%s] 检查资源目录: %s", project.getName(), resourceDirPath));
                        // 使用 LocalFileSystem 构建资源目录的绝对路径
                        VirtualFile resourceDirectory = LocalFileSystem.getInstance().findFileByPath(module.getPath() + "/" + resourceDirPath);

                        File file = null;
                        if (resourceDirectory != null) {
                            file = new File(resourceDirectory.getPath() + "/" + fileName);
                            Logger.info(String.format("[%s] 目标文件路径: %s", project.getName(), file.getPath()));
                        } else {
                            Logger.warn(String.format("[%s] 资源目录不存在: %s", project.getName(), resourceDirPath));
                            continue;
                        }

                        if (file != null && file.exists()) {
                            boolean delete = file.delete();
                            if (!delete) {
                                Logger.warn(String.format("[%s] 文件删除失败: %s", project.getName(), file.getPath()));
                            }
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
                                Logger.info(String.format("[%s] 文件下载成功: %s", project.getName(), file.getCanonicalPath()));
                                // 解压文件， 获取配置
                                extractFilesFromNestedZip(resourceDirectory.getPath() + "/" + fileName, resourceDirectory.getPath(), stackMode);
                            }
                        }
                    } catch (Exception e) {
                        Logger.error(String.format("[%s] 保存新版配置文件失败: %s", project.getName(), e.getMessage()));
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        Logger.info(String.format("[%s] 新版配置文件保存完成", project.getName()));
    }

    public void extractFilesFromNestedZip(String zipFilePath, String outputDir, boolean stackMode) throws IOException {
        Logger.info(String.format("[%s] 开始解压嵌套ZIP文件: %s", project.getName(), zipFilePath));
        String regex = "home/hundsun/*(/config|/cust-config)/";
        Pattern pattern = Pattern.compile(regex);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.getName().startsWith(project.getName() + "-svr")) {
                    continue;
                }
                Logger.info(String.format("[%s] 处理ZIP条目: %s", project.getName(), zipEntry.getName()));
                if (zipEntry.getName().endsWith(".zip")) {
                    File tempFile = File.createTempFile("tempZip", ".zip");
                    Logger.info(String.format("[%s] 创建临时ZIP文件: %s", project.getName(), tempFile.getPath()));
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
                                    Logger.info(String.format("[%s] 文件在排除列表中，跳过: %s", project.getName(), fileName));
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
                                        }
                                    } else {
                                        while ((length = nestedZis.read(buffer)) > 0) {
                                            String content = new String(buffer, 0, length);
                                            // 根据配置规则替换文本内容
                                            List<EasyEnvConfig.ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
                                            for (EasyEnvConfig.ConfigReplaceRule configReplaceRule : configReplaceRules) {
                                                if (CommonValidateUtil.isFileNameMatch(fileName, configReplaceRule.getFileName())) {
                                                    Logger.info(String.format("[%s] 应用替换规则: %s -> %s", project.getName(),
                                                            configReplaceRule.getRegExpression(), configReplaceRule.getReplaceStr()));
                                                    String regExpression = configReplaceRule.getRegExpression();
                                                    String replaceStr = configReplaceRule.getReplaceStr();

                                                    Pattern pattern1 = Pattern.compile(regExpression);
                                                    Matcher matcher1 = pattern1.matcher(content);
                                                    content = matcher1.replaceAll(replaceStr);
                                                }
                                            }
                                            fos.write(content.getBytes());
                                        }
                                    }
                                    Logger.info(String.format("[%s] 文件保存成功: %s", project.getName(), outputFile.getCanonicalPath()));
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
        // 判断是否在过滤列表内
        List<EasyEnvConfig.ExcludedFile> excludedFiles = config.getExcludedFiles();
        for (EasyEnvConfig.ExcludedFile excludedFile : excludedFiles) {
            String excludeFileName = excludedFile.getFileName();
            if (CommonValidateUtil.isFileNameMatch(fileName, excludeFileName)) {
                Logger.info(String.format("[%s] 文件在排除列表中，跳过: %s", project.getName(), fileName));
                return;
            }
        }

        // 应用配置替换规则
        List<EasyEnvConfig.ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
        for (EasyEnvConfig.ConfigReplaceRule configReplaceRule : configReplaceRules) {
            if (CommonValidateUtil.isFileNameMatch(fileName, configReplaceRule.getFileName())) {
                Logger.info(String.format("[%s] 应用替换规则: %s -> %s", project.getName(),
                        configReplaceRule.getRegExpression(), configReplaceRule.getReplaceStr()));
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
                    VirtualFile resourceDirectory = LocalFileSystem.getInstance().findFileByPath(module.getPath() + "/" + resourceDirPath);

                    if (resourceDirectory == null) {
                        continue;
                    }

                    File file = new File(resourceDirectory.getPath() + "/" + fileName);
                    if (file.exists()) {
                        if (!file.delete()) {
                            Logger.warn(String.format("[%s] 文件删除失败: %s", project.getName(), file.getPath()));
                        }
                    }

                    if (fileName.contains(".dat")) {
                        byte[] bytes = Base64.decodeBase64(content);
                        FileUtils.writeByteArrayToFile(file, bytes);
                    } else {
                        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
                    }
                    Logger.info(String.format("[%s] 文件保存成功: %s", project.getName(), file.getCanonicalPath()));

                    // 刷新资源目录
                    VfsUtil.markDirtyAndRefresh(true, true, true, resourceDirectory);
                } catch (IOException e) {
                    Logger.error(String.format("[%s] 保存文件失败: %s, 错误: %s",
                            project.getName(), fileName, e.getMessage()));
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
