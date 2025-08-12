package com.github.hanfeng21050.controller.export;

import com.github.hanfeng21050.exception.HepExportException;
import com.github.hanfeng21050.utils.Logger;
import com.google.gson.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hepbiz文件导出器
 * 用于将.hepbiz文件导出为OpenAPI格式
 */
public class OpenApiExporterController implements HepExporter {
    private final Project project;
    private final Map<String, JsonObject> typeMapping = new HashMap<>();
    private final Map<String, JsonObject> objCache = new HashMap<>();
    private final Map<String, JsonArray> categoryParams = new HashMap<>();
    private JsonObject openapi;

    /**
     * 构造函数
     *
     * @param project 当前IntelliJ项目实例
     */
    public OpenApiExporterController(Project project) {
        this.project = project;
    }

    /**
     * 加载类型定义文件
     * 从当前项目的resources目录下的type_mappings.json加载类型映射
     *
     * @throws IOException 当读取或解析类型定义文件时发生错误
     */
    private void loadTypeDefinitions() throws IOException {
        Logger.info("开始加载类型定义文件...");
        // 从当前类的ClassLoader加载资源文件
        try (InputStream inputStream = OpenApiExporterController.class.getResourceAsStream("/type_mappings.json")) {
            if (inputStream == null) {
                Logger.error("无法找到type_mappings.json文件");
                throw new IOException("无法找到type_mappings.json文件");
            }

            // 读取并解析JSON内容
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject mappings = JsonParser.parseString(content).getAsJsonObject();

            if (!mappings.has("typeMapping")) {
                Logger.error("type_mappings.json文件格式错误：缺少typeMapping字段");
                throw new IOException("type_mappings.json文件格式错误：缺少typeMapping字段");
            }

            JsonObject types = mappings.getAsJsonObject("typeMapping");
            typeMapping.clear(); // 清除旧的映射

            // 加载所有类型映射
            for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
                String typeName = entry.getKey();
                if (!entry.getValue().isJsonObject()) {
                    Logger.error(String.format("类型'%s'的定义不是有效的JSON对象", typeName));
                    throw new IOException(String.format("类型'%s'的定义不是有效的JSON对象", typeName));
                }
                JsonObject typeInfo = entry.getValue().getAsJsonObject();

                // 验证必要的字段
                if (!typeInfo.has("openApiType")) {
                    Logger.error(String.format("类型'%s'缺少openApiType字段", typeName));
                    throw new IOException(String.format("类型'%s'缺少openApiType字段", typeName));
                }

                typeMapping.put(typeName, typeInfo);
            }
            Logger.info("类型定义加载完成，共加载 " + typeMapping.size() + " 个类型");
        } catch (JsonSyntaxException e) {
            Logger.error("解析type_mappings.json文件时发生错误", e);
            throw new IOException("解析type_mappings.json文件时发生错误: " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.error("处理type_mappings.json文件时发生错误", e);
            throw new IOException("处理type_mappings.json文件时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 在项目的子模块中查找jresProject.xml文件
     *
     * @param project 当前项目
     * @return jresProject.xml文件
     * @throws HepExportException 如果找不到文件
     */
    private VirtualFile findJresProjectInModules(Project project) throws HepExportException {
        Logger.info("开始在项目模块中查找jresProject.xml文件...");
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            String moduleName = module.getName();
            Logger.info("检查模块: " + moduleName);
            if (moduleName.endsWith("-pub")) {
                VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
                for (VirtualFile root : contentRoots) {
                    VirtualFile jresFile = root.findFileByRelativePath("jresProject.xml");
                    if (jresFile != null && jresFile.exists()) {
                        String path = jresFile.getPath().replace('\\', '/');
                        Logger.info("找到jresProject.xml文件: " + path);
                        return jresFile;
                    }
                }
            }
        }
        Logger.error("在所有-pub模块中都找不到jresProject.xml文件");
        throw new HepExportException("在所有-pub模块中都找不到jresProject.xml文件");
    }

    /**
     * 加载默认参数
     *
     * @param project 当前项目
     * @throws HepExportException 加载异常
     */
    private void loadDefaultParams(Project project) throws HepExportException {
        try {
            VirtualFile jresFile = findJresProjectInModules(project);
            String content = new String(jresFile.contentsToByteArray(), StandardCharsets.UTF_8);
            JsonObject jresProject = JsonParser.parseString(content).getAsJsonObject();

            JsonObject extensibleModel = jresProject.getAsJsonObject("extensibleModel");
            if (extensibleModel == null) {
                throw new HepExportException("jresProject.xml中找不到extensibleModel节点");
            }

            JsonObject data2 = extensibleModel.getAsJsonObject("data2");
            if (data2 == null) {
                throw new HepExportException("jresProject.xml中找不到extensibleModel.data2节点");
            }

            if (data2.has("biz_extend_property")) {
                JsonObject bizExtendProperty = data2.getAsJsonObject("biz_extend_property");
                if (bizExtendProperty.has("cates")) {
                    JsonArray cates = bizExtendProperty.getAsJsonArray("cates");
                    for (JsonElement cateElement : cates) {
                        JsonObject cate = cateElement.getAsJsonObject();
                        if (cate.has("name") && cate.has("inParams")) {
                            String cateName = cate.get("name").getAsString();
                            JsonArray inParams = cate.getAsJsonArray("inParams");
                            categoryParams.put(cateName, inParams);
                        }
                    }
                }
            }

            if (categoryParams.isEmpty()) {
                Logger.warn("未在jresProject.xml中找到任何分类参数");
            } else {
                Logger.info("成功加载分类参数：" + String.join(", ", categoryParams.keySet()));
            }
        } catch (Exception e) {
            Logger.error("解析jresProject.xml时出错：" + e.getMessage());
            throw new HepExportException("解析jresProject.xml文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理输入参数，转换为OpenAPI格式
     *
     * @param parameters 输入参数数组
     * @param operation  当前API操作
     * @param category   接口类别
     */
    private void processInputParameters(JsonArray parameters, JsonObject operation, String category) {
        if (parameters == null) {
            parameters = new JsonArray();
        }

        // 获取该类别的默认参数
        JsonArray defaultParams = categoryParams.get(category);
        if (defaultParams != null) {
            // 创建一个新的参数数组，包含默认参数和自定义参数
            JsonArray mergedParams = new JsonArray();

            // 添加默认参数
            for (JsonElement defaultParam : defaultParams) {
                mergedParams.add(defaultParam);
            }

            // 添加自定义参数
            for (JsonElement param : parameters) {
                mergedParams.add(param);
            }

            parameters = mergedParams;
        }

        JsonObject requestBody = new JsonObject();
        JsonObject content = new JsonObject();
        JsonObject mediaType = new JsonObject();
        JsonObject schema = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();

        // 设置为对象类型
        schema.addProperty("type", "object");
        schema.add("properties", properties);

        for (JsonElement param : parameters) {
            JsonObject parameter = param.getAsJsonObject();

            // 检查必需字段是否存在
            if (!parameter.has("name")) {
                continue;
            }
            String paramName = parameter.get("name").getAsString();

            // 获取参数是否必需
            if (parameter.has("required") && parameter.get("required").getAsBoolean()) {
                required.add(paramName);
            }

            // 处理对象参数
            if (parameter.has("obj") && parameter.get("obj").getAsBoolean()) {
                if (!parameter.has("type")) {
                    continue;
                }
                String typeName = parameter.get("type").getAsString();
                JsonObject objSchema = processObject(typeName);
                if (objSchema != null) {
                    JsonObject paramSchema = new JsonObject();
                    paramSchema.addProperty("$ref", "#/components/schemas/" + typeName);

                    // 处理集合类型
                    if (parameter.has("collection") && parameter.get("collection").getAsBoolean()) {
                        JsonObject arraySchema = new JsonObject();
                        arraySchema.addProperty("type", "array");
                        arraySchema.add("items", paramSchema);
                        paramSchema = arraySchema;
                    }

                    properties.add(paramName, paramSchema);

                    // 将对象定义添加到components/schemas
                    if (!openapi.has("components")) {
                        openapi.add("components", new JsonObject());
                    }
                    if (!openapi.getAsJsonObject("components").has("schemas")) {
                        openapi.getAsJsonObject("components").add("schemas", new JsonObject());
                    }
                    openapi.getAsJsonObject("components").getAsJsonObject("schemas").add(typeName, objSchema);
                }
            } else {
                // 处理基本类型参数
                JsonObject paramSchema = convertParameter(parameter);
                properties.add(paramName, paramSchema);

                if (parameter.has("description")) {
                    paramSchema.addProperty("description", parameter.get("description").getAsString());
                }
            }
        }

        // 添加必需字段
        if (required.size() > 0) {
            schema.add("required", required);
        }

        mediaType.add("schema", schema);
        content.add("application/json", mediaType);
        requestBody.add("content", content);
        operation.add("requestBody", requestBody);
    }

    /**
     * 转换参数类型
     *
     * @param parameter 参数定义
     * @return OpenAPI格式的参数schema
     */
    private JsonObject convertParameter(JsonObject parameter) {
        JsonObject schema = new JsonObject();
        String type = parameter.has("type") ? parameter.get("type").getAsString() : "string";
        JsonObject typeInfo = typeMapping.get(type);

        // 处理集合类型
        if (parameter.has("collection") && parameter.get("collection").getAsBoolean()) {
            schema.addProperty("type", "array");
            JsonObject items = new JsonObject();
            if (typeInfo != null) {
                copyTypeProperties(typeInfo, items);
            } else {
                items.addProperty("type", mapHepTypeToOpenAPI(type));
            }
            schema.add("items", items);
        } else {
            if (typeInfo != null) {
                copyTypeProperties(typeInfo, schema);
            } else {
                schema.addProperty("type", mapHepTypeToOpenAPI(type));
            }
        }

        return schema;
    }

    private void copyTypeProperties(JsonObject typeInfo, JsonObject schema) {
        schema.addProperty("type", typeInfo.get("openApiType").getAsString());
        if (typeInfo.has("format")) {
            schema.addProperty("format", typeInfo.get("format").getAsString());
        }
        if (typeInfo.has("decimal")) {
            schema.addProperty("format", "decimal");
            schema.addProperty("multipleOf", Math.pow(10, -typeInfo.get("decimal").getAsInt()));
        }
    }

    /**
     * 处理对象类型，将Hepbiz对象类型转换为OpenAPI schema
     *
     * @param typeName 对象类型名称
     * @return OpenAPI格式的对象schema定义
     */
    private JsonObject processObject(String typeName) {
        // 首先检查缓存
        if (objCache.containsKey(typeName)) {
            return objCache.get(typeName);
        }

        // 查找对象定义文件
        VirtualFile objFile = findHepObjFile(typeName);
        if (objFile == null) {
            return null;
        }

        try {
            // 读取并解析对象定义
            String content = new String(objFile.contentsToByteArray(), StandardCharsets.UTF_8);
            JsonObject objDefinition = JsonParser.parseString(content).getAsJsonObject();
            if (!objDefinition.has("detail")) {
                return null;
            }
            JsonObject detail = objDefinition.getAsJsonObject("detail");

            if (detail == null || !detail.has("fields")) {
                return null;
            }

            // 创建OpenAPI schema
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");

            // 添加中文名称作为描述
            if (detail.has("chineseName")) {
                schema.addProperty("description", detail.get("chineseName").getAsString());
            }

            JsonObject properties = new JsonObject();
            JsonArray required = new JsonArray();

            // 处理字段
            JsonArray fields = detail.getAsJsonArray("fields");
            for (JsonElement fieldElement : fields) {
                if (!fieldElement.isJsonObject()) {
                    continue;
                }
                JsonObject field = fieldElement.getAsJsonObject();
                if (!field.has("name") || !field.has("type")) {
                    continue;
                }
                String fieldName = field.get("name").getAsString();
                boolean isRequired = field.has("necessarily") && field.get("necessarily").getAsBoolean();

                // 创建字段schema
                JsonObject fieldSchema = new JsonObject();

                // 处理集合类型
                boolean isCollection = field.has("collection") && field.get("collection").getAsBoolean();

                if (field.has("obj") && field.get("obj").getAsBoolean()) {
                    // 处理对象类型字段
                    String fieldTypeName = field.get("type").getAsString();
                    if (fieldTypeName.endsWith("DTO") || fieldTypeName.endsWith("Obj")) {
                        JsonObject nestedSchema = processObject(fieldTypeName);
                        if (nestedSchema != null) {
                            // 将嵌套对象添加到components/schemas
                            if (!openapi.has("components")) {
                                openapi.add("components", new JsonObject());
                            }
                            if (!openapi.getAsJsonObject("components").has("schemas")) {
                                openapi.getAsJsonObject("components").add("schemas", new JsonObject());
                            }
                            openapi.getAsJsonObject("components").getAsJsonObject("schemas").add(fieldTypeName, nestedSchema);

                            // 创建引用
                            JsonObject refSchema = new JsonObject();
                            refSchema.addProperty("$ref", "#/components/schemas/" + fieldTypeName);

                            if (isCollection) {
                                fieldSchema.addProperty("type", "array");
                                fieldSchema.add("items", refSchema);
                            } else {
                                fieldSchema = refSchema;
                            }
                        }
                    }
                } else {
                    // 处理基本类型字段
                    String fieldType = field.get("type").getAsString();
                    if (isCollection) {
                        fieldSchema.addProperty("type", "array");
                        JsonObject items = new JsonObject();
                        items.addProperty("type", mapHepTypeToOpenAPI(fieldType));
                        fieldSchema.add("items", items);
                    } else {
                        fieldSchema.addProperty("type", mapHepTypeToOpenAPI(fieldType));
                    }
                }

                properties.add(fieldName, fieldSchema);
                if (isRequired) {
                    required.add(fieldName);
                }
            }

            schema.add("properties", properties);
            if (required.size() > 0) {
                schema.add("required", required);
            }

            // 缓存结果
            objCache.put(typeName, schema);
            return schema;

        } catch (IOException e) {
            Logger.error("读取对象定义文件时出错", e);
            return null;
        }
    }

    /**
     * 查找对象定义文件
     *
     * @param typeName 类型名称
     * @return 对象定义文件
     */
    private VirtualFile findHepObjFile(String typeName) {
        if (project == null) {
            return null;
        }

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        // 搜索所有xxx-pub目录
        VirtualFile[] children = baseDir.getChildren();
        for (VirtualFile child : children) {
            if (child.isDirectory() && child.getName().endsWith("-pub")) {
                // 查找studio-resources/objects目录
                VirtualFile studioResources = child.findChild("studio-resources");
                if (studioResources != null && studioResources.isDirectory()) {
                    VirtualFile objectsDir = studioResources.findChild("objects");
                    if (objectsDir != null && objectsDir.isDirectory()) {
                        // 递归搜索objects目录及其子目录
                        VirtualFile objFile = findFileRecursively(objectsDir, typeName + ".hepobj");
                        if (objFile != null && !objFile.isDirectory()) {
                            return objFile;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 递归搜索目录查找文件
     *
     * @param dir      要搜索的目录
     * @param fileName 要查找的文件名
     * @return 找到的文件，如果未找到返回null
     */
    private VirtualFile findFileRecursively(VirtualFile dir, String fileName) {
        VirtualFile[] children = dir.getChildren();
        for (VirtualFile child : children) {
            if (!child.isDirectory() && child.getName().equals(fileName)) {
                return child;
            }
            if (child.isDirectory()) {
                VirtualFile result = findFileRecursively(child, fileName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * 将Hep类型映射到OpenAPI类型
     *
     * @param hepType Hep类型名称
     * @return OpenAPI类型
     */
    private String mapHepTypeToOpenAPI(String hepType) {
        JsonObject typeInfo = typeMapping.get(hepType);
        if (typeInfo != null && typeInfo.has("openApiType")) {
            return typeInfo.get("openApiType").getAsString();
        }
        return "string"; // 默认类型
    }

    /**
     * 从文件路径获取接口分组
     *
     * @param file       文件
     * @param projectName 项目名称
     * @return 分组标签数组
     */
    private JsonArray getApiTags(VirtualFile file, String projectName) {
        JsonArray tags = new JsonArray();
        String path = file.getPath();

        // 添加项目名称作为第一层分组
        tags.add(projectName);

        // 查找studio-resources目录的位置
        int studioIndex = path.indexOf("studio-resources");
        if (studioIndex == -1) {
            return tags;
        }

        // 获取studio-resources/biz之后的路径
        String relativePath = path.substring(studioIndex);
        String[] parts = relativePath.split("/");

        StringBuilder groupPath = new StringBuilder(projectName);
        for (int i = 2; i < parts.length - 1; i++) { // 跳过studio-resources和biz，以及最后的文件名
            groupPath.append("/").append(parts[i]);
            tags.add(groupPath.toString());
        }

        return tags;
    }

    /**
     * 为OpenAPI操作添加标准响应schema
     * 添加统一的响应结构，包含error_no和error_info字段
     *
     * @param operation 当前API操作的JsonObject
     * @param schemas   全局schema定义
     */
    private void addResponseSchema(JsonObject operation, JsonObject schemas) {
        // 创建responses对象，用于存储所有响应定义
        JsonObject responses = new JsonObject();
        JsonObject response = new JsonObject();
        response.addProperty("description", "Successful response");

        // 创建响应的schema结构
        JsonObject responseSchema = new JsonObject();
        responseSchema.addProperty("type", "object");
        JsonObject responseProperties = new JsonObject();

        // 添加错误码字段
        JsonObject errorNo = new JsonObject();
        errorNo.addProperty("type", "integer");
        errorNo.addProperty("description", "错误码，0表示成功");
        responseProperties.add("error_no", errorNo);

        // 添加错误信息字段
        JsonObject errorInfo = new JsonObject();
        errorInfo.addProperty("type", "string");
        errorInfo.addProperty("description", "错误信息");
        responseProperties.add("error_info", errorInfo);

        // 组装响应schema
        responseSchema.add("properties", responseProperties);

        // 设置响应的content type和schema
        JsonObject content = new JsonObject();
        JsonObject jsonContent = new JsonObject();
        jsonContent.add("schema", responseSchema);
        content.add("application/json", jsonContent);
        response.add("content", content);

        // 将200响应添加到responses中
        responses.add("200", response);
        operation.add("responses", responses);
    }

    /**
     * 导出OpenAPI文档
     *
     * @param project 项目
     * @param files   文件列表
     * @return 导出的OpenAPI文档内容
     */
    public String exportToOpenAPI(Project project, List<VirtualFile> files) throws HepExportException {
        Logger.info("开始导出OpenAPI文档...");
        try {
            // 加载类型定义和默认参数
            loadTypeDefinitions();
            loadDefaultParams(project);

            Logger.info("开始处理 " + files.size() + " 个文件");
            
            // 创建OpenAPI文档
            JsonObject openapi = new JsonObject();
            this.openapi = openapi;

            // 设置基本信息
            openapi.addProperty("openapi", "3.0.0");
            JsonObject info = new JsonObject();
            info.addProperty("title", "API Documentation");
            info.addProperty("version", "1.0.0");
            openapi.add("info", info);

            // 初始化paths对象
            openapi.add("paths", new JsonObject());

            // 初始化components和schemas对象
            JsonObject components = new JsonObject();
            JsonObject schemas = new JsonObject();
            components.add("schemas", schemas);
            openapi.add("components", components);

            // 初始化tags数组
            openapi.add("tags", new JsonArray());

            // 处理每个文件
            int processedCount = 0;
            for (VirtualFile file : files) {
                Logger.info("处理文件 (" + (++processedCount) + "/" + files.size() + "): " + file.getPath());
                processFile(file, project, openapi);
            }

            // 导出到文件
            String result = exportToFile(openapi, project);
            if (result != null) {
                Logger.info("OpenAPI文档导出成功");
            } else {
                Logger.warn("用户取消了文件导出");
            }
            return result;

        } catch (Exception e) {
            Logger.error("生成OpenAPI文档失败", e);
            throw new HepExportException("生成OpenAPI文档失败: " + e.getMessage());
        }
    }

    /**
     * 处理单个文件
     */
    private void processFile(VirtualFile file, Project project, JsonObject openapi) throws IOException, HepExportException {
        // 解析Hepbiz文件内容
        String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        JsonObject hepbizJson = JsonParser.parseString(content).getAsJsonObject();
        if (!hepbizJson.has("detail")) {
            throw new HepExportException("文件缺少detail字段: " + file.getName());
        }

        JsonObject detail = hepbizJson.getAsJsonObject("detail");
        JsonObject basicInfo = hepbizJson.getAsJsonObject("basic_info");

        // 获取API路径
        if (!detail.has("httpURL") && !basicInfo.has("name")) {
            return;
        }
        String path = detail.has("httpURL") ? detail.get("httpURL").getAsString() : basicInfo.get("name").getAsString();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // 获取项目名并处理格式
        String projectPath = project.getName();
        String projectPathNew = projectPath.replaceFirst("-", ".");

        // 拼接最终路径 /g/项目名/v/接口名
        path = "/g/hs" + projectPathNew + "/v" + path;

        // 创建API操作对象
        JsonObject pathItem = new JsonObject();
        JsonObject operation = new JsonObject();

        // 添加API摘要信息
        if (detail.has("chineseName")) {
            operation.addProperty("summary", detail.get("chineseName").getAsString());
        }

        // 处理API标签
        processApiTags(file, project.getName(), operation, openapi);

        // 处理请求参数
        if (detail.has("inputs")) {
            JsonArray inputs = detail.getAsJsonArray("inputs");
            String category = determineCategory(detail, file);
            processInputParameters(inputs, operation, category);
        }

        // 添加响应定义
        addResponseSchema(operation, openapi.getAsJsonObject("components").getAsJsonObject("schemas"));

        // 设置HTTP方法
        String httpMethod = determineHttpMethod(detail);

        // 添加operationId和描述
        if (!detail.has("name")) {
            throw new HepExportException("文件缺少name字段: " + file.getName());
        }
        operation.addProperty("operationId", detail.get("name").getAsString());
        if (detail.has("description")) {
            operation.addProperty("description", detail.get("description").getAsString());
        }

        // 将操作添加到路径中
        pathItem.add(httpMethod, operation);
        if (!openapi.getAsJsonObject("paths").has(path)) {
            openapi.getAsJsonObject("paths").add(path, pathItem);
        } else {
            openapi.getAsJsonObject("paths").getAsJsonObject(path).add(httpMethod, operation);
        }
    }

    /**
     * 确定API类别
     */
    private String determineCategory(JsonObject detail, VirtualFile file) {
        if (detail.has("category")) {
            return detail.get("category").getAsString();
        }

        String filePath = file.getPath().toLowerCase();
        if (filePath.contains("/inner/") || filePath.contains("\\inner\\")) {
            return "inner";
        } else if (filePath.contains("/ext/") || filePath.contains("\\ext\\")) {
            return "ext";
        } else if (filePath.contains("/out/") || filePath.contains("\\out\\")) {
            return "out";
        }
        return "biz";
    }

    /**
     * 确定HTTP方法
     */
    private String determineHttpMethod(JsonObject detail) {
        if (detail.has("httpMethod")) {
            JsonElement httpMethodElement = detail.get("httpMethod");
            if (httpMethodElement != null && !httpMethodElement.isJsonNull()) {
                return httpMethodElement.getAsString().toLowerCase();
            }
        }
        return "post";
    }

    /**
     * 处理API标签
     */
    private void processApiTags(VirtualFile file, String projectName, JsonObject operation, JsonObject openapi) {
        JsonArray fileTags = getApiTags(file, projectName);
        if (fileTags.size() > 0) {
            // 添加新的标签到全局tags
            for (JsonElement tag : fileTags) {
                String tagName = tag.getAsString();
                if (!openapi.has("tags")) {
                    openapi.add("tags", new JsonArray());
                }
                JsonArray tags = openapi.getAsJsonArray("tags");
                boolean found = false;
                for (JsonElement existingTag : tags) {
                    if (existingTag.getAsJsonObject().get("name").getAsString().equals(tagName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JsonObject tagObject = new JsonObject();
                    tagObject.addProperty("name", tagName);
                    // 如果是项目名称，使用特殊描述
                    if (tagName.equals(projectName)) {
                        tagObject.addProperty("description", "APIs in project: " + projectName);
                    } else {
                        tagObject.addProperty("description", "API group: " + tagName);
                    }
                    tags.add(tagObject);
                }
            }
            operation.add("tags", fileTags);
        }
    }

    /**
     * 导出OpenAPI文档到文件
     *
     * @param openapi OpenAPI文档
     * @param project 当前项目
     * @return 导出的文件内容
     */
    private String exportToFile(JsonObject openapi, Project project) throws IOException {
        Logger.info("准备导出OpenAPI文档到文件...");
        
        // 获取当前时间戳
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 获取项目名称并构建默认文件名
        String projectName = project.getName();
        String defaultFileName = String.format("%s-openapi-%s.json", projectName, timestamp);

        // 配置文件选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择导出位置");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));

        // 显示保存对话框
        if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            Logger.info("用户取消了文件保存");
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
        }

        Logger.info("选择的导出文件路径: " + selectedFile.getAbsolutePath());

        // 检查文件是否已存在
        if (selectedFile.exists()) {
            int response = JOptionPane.showConfirmDialog(null,
                    "文件已存在，是否覆盖？",
                    "确认覆盖",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (response != JOptionPane.YES_OPTION) {
                Logger.info("用户取消了文件覆盖");
                return null;
            }
        }

        // 使用try-with-resources确保资源正确关闭
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(openapi);

        try (FileWriter writer = new FileWriter(selectedFile, StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
            Logger.info("文件导出成功: " + selectedFile.getAbsolutePath());
            return jsonOutput;
        } catch (IOException e) {
            Logger.error("文件导出失败", e);
            throw e;
        }
    }

    @Override
    public String export(Project project, List<VirtualFile> files) throws HepExportException {
        return exportToOpenAPI(project, files);
    }

    @Override
    public String getName() {
        return "OpenAPI";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public String getFileTypeDescription() {
        return "OpenAPI JSON files";
    }
}
