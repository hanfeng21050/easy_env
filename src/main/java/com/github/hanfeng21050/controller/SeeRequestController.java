package com.github.hanfeng21050.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.utils.HttpClientUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeeRequestController {
    private static final Logger LOGGER = Logger.getInstance(SeeRequestController.class);
    // 常量定义
    private static final String SUCCESS = "success";
    private static final String CAS_LOGIN_URL = "/cas/login";
    private static final String ACM_URL = "/acm";
    private static final String ACM_SYSTEM_AUTH_JSON_URL = "/acm/system/auth.json";
    private static final String ACM_DSSP_APPLICATION_QUERY_JSON_URL = "/acm/dssp/application/authority/query.json";
    private static final String ACM_DSSP_CONFIG_GET_COMPARE_CONFIG_JSON_URL = "/acm/dssp/config/getCompareConfig.json";
    private static final String ACM_BROKER_UF30DEPLOY_EXPORTAPPCONFIG_URL = "/acm/broker/uf30Deploy/exportAppConfig.json";
    private static final String ACM_APPLICATION_COMPUTERSTATUS_UF30ANDXONEAPPS_URL = "/acm/application/computerStatus/uf30AndXoneApps.json";
    private static final String ACM_BROKER_APPMENU_LOCALCACHEFORMDATAONLYCOMPUTER_URL = "/acm/broker/appMenu/localCacheFormDataOnlyComputer.json";
    private static final String ACM_BROKER_APPMENU_LOCALCACHEFORMDATAONLYTABLE_URL = "/acm/broker/appMenu/localCacheFormDataOnlyTable.json";
    private static final String ACM_GOVERNANCE_SERVICE_PAGESERVICELIST_URL = "/acm/governance/service/pageServiceList.json";
    private static final String ACM_GOVERNANCE_SERVICE_SERVICEINFOQUERY_URL = "/acm/governance/service/serviceInfoQuery.json";
    private static final String ACM_BROKER_APPMENU_LOACALCACHEREFRESH_URL = "/acm/broker/appMenu/localCacheRefresh.json";
    private static final String ACM_HSSERVER_APP_GETAPPDETAIL_URL = "/acm/hsserver/app/getAppDetail.json";


    // 登录方法
    public static void login(SeeConfig seeConfig) throws Exception {
        HttpClientUtil.clearCookie();

        // 获取登录页面
        String loginUrl = seeConfig.getAddress() + CAS_LOGIN_URL + "?get-lt=true";
        String response = HttpClientUtil.httpGet(loginUrl);

        // 从响应中提取 lt 和 execution 的值

        String ltValue;
        String executionValue;

        ltValue = extractValueFromResponse(response, "name=\"lt\" value=\"(.*?)\"");
        executionValue = extractValueFromResponse(response, "name=\"execution\" value=\"(.*?)\"");

        if (StringUtils.isBlank(ltValue)) {
            ltValue = extractValueFromResponse(response, "\"lt\":\"(.*?)\"");
            executionValue = extractValueFromResponse(response, "\"execution\":\"(.*?)\"");
        }


        // 构建登录参数
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", seeConfig.getUsername());
        loginParams.put("password", seeConfig.getPassword());
        loginParams.put("execution", executionValue);
        loginParams.put("lt", ltValue);
        loginParams.put("submit", "LOGIN");
        loginParams.put("_eventId", "submit");

        // 发起登录请求
        String res = HttpClientUtil.httpPost(seeConfig.getAddress() + CAS_LOGIN_URL, loginParams);

        if (SUCCESS.equals(res)) {
            // 页面跳转，获取cookie
            String s = HttpClientUtil.httpGet(seeConfig.getAddress() + ACM_URL);
        } else {
            throw new Exception("用户验证失败");
        }
    }

    // 获取认证信息方法
    public static String getAuth(SeeConfig seeConfig) throws URISyntaxException, IOException {
        String authJsonUrl = seeConfig.getAddress() + ACM_SYSTEM_AUTH_JSON_URL;
        return extractValueFromResponse(HttpClientUtil.httpPost(authJsonUrl, new HashMap<>()), "\"token\":\"(.*?)\"");
    }

    // 从响应中提取指定模式的值
    public static String extractValueFromResponse(String response, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * 从轻量化合并包中获取
     *
     * @param seeConfig
     * @param auth
     * @return
     * @throws IOException
     */
    public static String getStackApplication(SeeConfig seeConfig, String applicationName, String auth) throws IOException, URISyntaxException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        body.put("pageNo", "1");
        body.put("pageSize", "100");
        body.put("category", "stack");
        body.put("allowedUpgradeMark", "true");
        header.put("Authorization", "Bearer " + auth);

        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_DSSP_APPLICATION_QUERY_JSON_URL, body, header);
        JSONObject parse = JSONObject.parse(response);

        // 处理响应
        String errorInfo = parse.getString("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("items");
            if (!jsonArray.isEmpty()) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id = jsonObject.getString("id");
                    String productId = jsonObject.getString("productId");
                    boolean b = existMacroSvr(seeConfig, auth, id, productId, applicationName);
                    if (b) {
                        return id;
                    }
                }
            }
        } else {
            // 处理错误信息
            handleError(errorInfo);
        }
        return "";

    }

    /**
     * 判断微服务是否在合并包中
     *
     * @param seeConfig
     * @param auth
     * @param appId
     * @param applicationName
     * @return
     * @throws IOException
     */
    private static boolean existMacroSvr(SeeConfig seeConfig, String auth, String appId, String productId, String applicationName) throws URISyntaxException, IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("appId", appId);
        body.put("instance_id", productId);

        String response = HttpClientUtil.httpGet(seeConfig.getAddress() + ACM_HSSERVER_APP_GETAPPDETAIL_URL, body, header);

        JSONObject parse = JSONObject.parse(response);
        // 处理响应
        String errorInfo = parse.getString("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("subProductInfoList");
            if (!jsonArray.isEmpty()) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String productTypeName = jsonObject.getString("productTypeName");
                    if (applicationName.equals(productTypeName)) {
                        return true;
                    }
                }
            }
        } else {
            // 处理错误信息
            handleError(errorInfo);
        }
        return false;
    }

    // 获取应用信息方法
    public static String getApplication(SeeConfig seeConfig, String applicationName, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        body.put("pageNo", "1");
        body.put("pageSize", "1");
        body.put("name", applicationName);
        body.put("allowedUpgradeMark", "true");
        header.put("Authorization", "Bearer " + auth);

        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_DSSP_APPLICATION_QUERY_JSON_URL, body, header);
        JSONObject parse = JSONObject.parse(response);

        // 处理响应
        String errorInfo = parse.getString("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("items");
            if (!jsonArray.isEmpty()) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                return jsonObject.getString("id");
            }
        } else {
            // 处理错误信息
            handleError(errorInfo);
        }
        return "";
    }

    // 获取配置文件信息
    public static JSONObject getConfigInfo(SeeConfig seeConfig, String applicationId, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("applicationId", applicationId);

        // 发起获取配置信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_DSSP_CONFIG_GET_COMPARE_CONFIG_JSON_URL, body, header);
        return JSONObject.parse(response);
    }

    // 获取新版配置文件
    public static JSONObject getConfigInfoNew(SeeConfig seeConfig, String applicationId, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("appId", applicationId);

        // 发起获取配置信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_BROKER_UF30DEPLOY_EXPORTAPPCONFIG_URL, body, header);
        return JSONObject.parse(response);
    }

    public static JSONObject getUf30AndXoneApps(SeeConfig seeConfig, String auth) throws IOException, URISyntaxException {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpGet(seeConfig.getAddress() + ACM_APPLICATION_COMPUTERSTATUS_UF30ANDXONEAPPS_URL, null, header);
        return JSONObject.parse(response);
    }

    public static JSONObject getLocalCacheFormDataOnlyComputer(SeeConfig seeConfig, String auth, String appId) throws IOException, URISyntaxException {
        Map<String, String> header = new HashMap<>();
        Map<String, String> params = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        params.put("appId", appId);
        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpGet(seeConfig.getAddress() + ACM_BROKER_APPMENU_LOCALCACHEFORMDATAONLYCOMPUTER_URL, params, header);
        return JSONObject.parse(response);
    }

    public static JSONObject getLocalCacheFormDataOnlyTable(SeeConfig seeConfig, String auth, String ip, String port) throws IOException, URISyntaxException {
        Map<String, String> header = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("ip", ip);
        body.put("port", port);
        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPostJSON(seeConfig.getAddress() + ACM_BROKER_APPMENU_LOCALCACHEFORMDATAONLYTABLE_URL, body, header);
        return JSONObject.parse(response);
    }


    public static JSONObject getCacheData(String url, Map<String, Object> params) throws IOException, URISyntaxException {
        Map<String, String> header = new HashMap<>();
        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPostJSON(url, params, header);
        return JSONObject.parse(response);
    }


    // 处理错误信息方法
    private static void handleError(String errorInfo) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showErrorDialog("error: " + errorInfo, "错误");
        });
        throw new RuntimeException(errorInfo);
    }

    public static JSONObject getServiceList(SeeConfig seeConfig, String applicationId, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("appId", applicationId);
        body.put("offset", "0");
        body.put("limit", "9999");
        body.put("runStatus", "running");

        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_GOVERNANCE_SERVICE_PAGESERVICELIST_URL, body, header);
        JSONObject parse = JSONObject.parse(response);
        return parse;
    }

    public static JSONObject getServiceInfo(SeeConfig seeConfig, String auth, Map<String, String> body) throws IOException {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);

        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_GOVERNANCE_SERVICE_SERVICEINFOQUERY_URL, body, header);
        return JSONObject.parse(response);
    }

    public static JSONObject localCacheRefresh(SeeConfig seeConfig, String auth, Map<String, Object> body) throws IOException {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);

        // 发起获取应用信息的请求
        String response = HttpClientUtil.httpPostJSON(seeConfig.getAddress() + ACM_BROKER_APPMENU_LOACALCACHEREFRESH_URL, body, header);
        return JSONObject.parse(response);
    }
}
