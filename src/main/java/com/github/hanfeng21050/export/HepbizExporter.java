package com.github.hanfeng21050.export;

import com.github.hanfeng21050.export.exception.HepbizExportException;
import com.google.gson.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hepbiz文件导出器
 * 用于将.hepbiz文件导出为OpenAPI格式
 */
public class HepbizExporter {
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
    public HepbizExporter(Project project) {
        this.project = project;
    }

    /**
     * 加载类型定义文件
     * 从当前项目的resources目录下的type_mappings.json加载类型映射
     *
     * @throws IOException 当读取或解析类型定义文件时发生错误
     */
    private void loadTypeDefinitions() throws IOException {
        // 从当前类的ClassLoader加载资源文件
        try (InputStream inputStream = HepbizExporter.class.getResourceAsStream("/type_mappings.json")) {
            if (inputStream == null) {
                throw new IOException("无法找到type_mappings.json文件");
            }

            // 读取并解析JSON内容
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject mappings = JsonParser.parseString(content).getAsJsonObject();

            if (!mappings.has("typeMapping")) {
                throw new IOException("type_mappings.json文件格式错误：缺少typeMapping字段");
            }

            JsonObject types = mappings.getAsJsonObject("typeMapping");
            typeMapping.clear(); // 清除旧的映射

            // 加载所有类型映射
            for (Map.Entry<String, JsonElement> entry : types.entrySet()) {
                String typeName = entry.getKey();
                if (!entry.getValue().isJsonObject()) {
                    throw new IOException(String.format("类型'%s'的定义不是有效的JSON对象", typeName));
                }
                JsonObject typeInfo = entry.getValue().getAsJsonObject();

                // 验证必要的字段
                if (!typeInfo.has("openApiType")) {
                    throw new IOException(String.format("类型'%s'缺少openApiType字段", typeName));
                }

                typeMapping.put(typeName, typeInfo);
            }
        } catch (JsonSyntaxException e) {
            throw new IOException("解析type_mappings.json文件时发生错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("处理type_mappings.json文件时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 在项目的子模块中查找jresProject.xml文件
     *
     * @param project 当前项目
     * @return jresProject.xml文件
     * @throws HepbizExportException 如果找不到文件
     */
    private VirtualFile findJresProjectInModules(Project project) throws HepbizExportException {
        Module[] modules = ModuleManager.getInstance(project).getModules();

        for (Module module : modules) {
            String moduleName = module.getName();
            if (moduleName.endsWith("-pub")) {
                VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
                for (VirtualFile root : contentRoots) {
                    VirtualFile jresFile = root.findFileByRelativePath("jresProject.xml");
                    if (jresFile != null && jresFile.exists()) {
                        System.out.println("在模块 " + moduleName + " 中找到jresProject.xml文件");
                        return jresFile;
                    }
                }
            }
        }
        throw new HepbizExportException("在所有-pub模块中都找不到jresProject.xml文件");
    }

    /**
     * 加载默认参数
     *
     * @param project 当前项目
     * @throws HepbizExportException 加载异常
     */
    private void loadDefaultParams(Project project) throws HepbizExportException {
        try {
            VirtualFile jresFile = findJresProjectInModules(project);
            String content = new String(jresFile.contentsToByteArray(), StandardCharsets.UTF_8);
            JsonObject jresProject = JsonParser.parseString(content).getAsJsonObject();

            JsonObject extensibleModel = jresProject.getAsJsonObject("extensibleModel");
            if (extensibleModel == null) {
                throw new HepbizExportException("jresProject.xml中找不到extensibleModel节点");
            }

            JsonObject data2 = extensibleModel.getAsJsonObject("data2");
            if (data2 == null) {
                throw new HepbizExportException("jresProject.xml中找不到extensibleModel.data2节点");
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
                System.out.println("警告：未在jresProject.xml中找到任何分类参数");
            } else {
                System.out.println("成功加载分类参数：" + String.join(", ", categoryParams.keySet()));
            }
        } catch (Exception e) {
            System.out.println("解析jresProject.xml时出错：" + e.getMessage());
            throw new HepbizExportException("解析jresProject.xml文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理输入参数，转换为OpenAPI格式
     *
     * @param parameters 输入参数数组
     * @param operation  当前API操作
     */
    private void processInputParameters(JsonArray parameters, JsonObject operation) {
        if (parameters == null || parameters.isEmpty()) {
            return;
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
        if (typeInfo.has("length")) {
            schema.addProperty("maxLength", typeInfo.get("length").getAsInt());
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

                    // 添加长度限制
                    if (field.has("length")) {
                        fieldSchema.addProperty("maxLength", Integer.parseInt(field.get("length").getAsString()));
                    }
                }

                properties.add(fieldName, fieldSchema);
                if (isRequired) {
                    required.add(fieldName);
                }
            }

            schema.add("properties", properties);
            if (!required.isEmpty()) {
                schema.add("required", required);
            }

            // 缓存结果
            objCache.put(typeName, schema);
            return schema;

        } catch (IOException e) {
            e.printStackTrace();
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
     * 获取文件所在的目录类型
     *
     * @param file 文件
     * @return 目录类型（inner, ext, out, biz）
     */
    private String getFileCategory(VirtualFile file) {
        String path = file.getPath().toLowerCase();
        if (path.contains("/inner/") || path.contains("\\inner\\")) {
            return "inner";
        } else if (path.contains("/ext/") || path.contains("\\ext\\")) {
            return "ext";
        } else if (path.contains("/out/") || path.contains("\\out\\")) {
            return "out";
        }
        return "biz";
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
     * 将Hepbiz文件导出为OpenAPI 3.0格式的文档
     *
     * @param project     当前IntelliJ项目实例
     * @param hepbizFiles 需要导出的Hepbiz文件列表
     * @return OpenAPI格式的JSON字符串
     * @throws HepbizExportException 当导出过程中发生错误时抛出
     */
    public String exportToOpenAPI(Project project, List<VirtualFile> hepbizFiles) throws HepbizExportException, IOException {
        // 加载类型定义和默认参数
        loadTypeDefinitions();
        loadDefaultParams(project);

        // 创建OpenAPI根对象
        openapi = new JsonObject();
        openapi.addProperty("openapi", "3.0.0");

        // 构建API文档基本信息
        JsonObject info = new JsonObject();
        info.addProperty("title", "Hepbiz API");
        info.addProperty("version", "1.0.0");
        info.addProperty("description", "恒生电子期货智能运营平台API");

        // 添加联系人信息
        JsonObject contact = new JsonObject();
        contact.addProperty("name", "恒生电子");
        contact.addProperty("url", "https://www.hundsun.com");
        info.add("contact", contact);

        // 添加许可证信息
        JsonObject license = new JsonObject();
        license.addProperty("name", "Hundsun License");
        info.add("license", license);

        openapi.add("info", info);

        // 创建paths和schemas对象，用于存储API路径和数据模型
        JsonObject paths = new JsonObject();
        JsonObject components = new JsonObject();
        JsonObject schemas = new JsonObject();
        components.add("schemas", schemas);
        openapi.add("components", components);

        // 处理每个Hepbiz文件
        for (VirtualFile file : hepbizFiles) {
            try {
                // 解析Hepbiz文件内容
                String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                JsonObject hepbizJson = JsonParser.parseString(content).getAsJsonObject();
                if (!hepbizJson.has("detail")) {
                    throw new HepbizExportException("文件缺少detail字段: " + file.getName());
                }
                JsonObject detail = hepbizJson.getAsJsonObject("detail");

                // 获取API路径
                if (!detail.has("httpURL")) {
                    throw new HepbizExportException("文件缺少httpURL字段: " + file.getName());
                }
                String path = detail.get("httpURL").getAsString();
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }

                // 创建API操作对象
                JsonObject pathItem = new JsonObject();
                JsonObject operation = new JsonObject();

                // 添加API摘要信息
                if (detail.has("chineseName")) {
                    operation.addProperty("summary", detail.get("chineseName").getAsString());
                }

                // 处理API标签
                JsonArray tags = new JsonArray();
                String category = getFileCategory(file);
                tags.add(getCategoryTag(category));
                operation.add("tags", tags);

                // 处理请求参数
                if (detail.has("inputs")) {
                    JsonArray inputs = detail.getAsJsonArray("inputs");
                    processInputParameters(inputs, operation);
                }

                // 添加响应定义
                addResponseSchema(operation, schemas);

                // 设置HTTP方法，默认为post
                String httpMethod = "post";
                if (detail.has("httpMethod")) {
                    JsonElement httpMethodElement = detail.get("httpMethod");
                    if (httpMethodElement != null && !httpMethodElement.isJsonNull()) {
                        httpMethod = httpMethodElement.getAsString().toLowerCase();
                    }
                }

                // 添加operationId和描述
                if (!detail.has("name")) {
                    throw new HepbizExportException("文件缺少name字段: " + file.getName());
                }
                operation.addProperty("operationId", detail.get("name").getAsString());
                if (detail.has("description")) {
                    operation.addProperty("description", detail.get("description").getAsString());
                }

                // 将操作添加到路径中
                pathItem.add(httpMethod, operation);
                paths.add(path, pathItem);
            } catch (IOException e) {
                throw new HepbizExportException("Failed to read hepbiz file: " + file.getName(), e);
            }
        }

        // 将paths添加到OpenAPI文档中
        openapi.add("paths", paths);

        return openapi.toString();
    }

    /**
     * 获取类别对应的标签名称
     *
     * @param category 类别名称
     * @return 标签名称
     */
    private String getCategoryTag(String category) {
        switch (category) {
            case "inner":
                return "内部接口";
            case "ext":
                return "外部接口";
            case "out":
                return "出参接口";
            case "biz":
                return "业务接口";
            default:
                return "其他接口";
        }
    }
}
