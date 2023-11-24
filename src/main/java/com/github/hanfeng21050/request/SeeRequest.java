package com.github.hanfeng21050.request;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.utils.CryptUtil;
import com.github.hanfeng21050.utils.HttpClientUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeeRequest {

    // ��������
    private static final String SUCCESS = "success";
    private static final String CAS_LOGIN_URL = "/cas/login";
    private static final String ACM_SYSTEM_AUTH_JSON_URL = "/acm/system/auth.json";
    private static final String ACM_DSSP_APPLICATION_QUERY_JSON_URL = "/acm/dssp/application/authority/query.json";
    private static final String ACM_DSSP_CONFIG_GET_COMPARE_CONFIG_JSON_URL = "/acm/dssp/config/getCompareConfig.json";

    // ��¼����
    public static void login(SeeConfig seeConfig) throws Exception {
        HttpClientUtil.clearCookie();

        // ��ȡ��¼ҳ��
        String loginUrl = seeConfig.getAddress() + CAS_LOGIN_URL + "?get-lt=true";
        String response = HttpClientUtil.httpGet(loginUrl);

        // ����Ӧ����ȡ lt �� execution ��ֵ
        String ltValue = extractValueFromResponse(response, "\"lt\":\"(.*?)\"");
        String executionValue = extractValueFromResponse(response, "\"execution\":\"(.*?)\"");

        // ������¼����
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", seeConfig.getUsername());
        loginParams.put("password", seeConfig.getPassword());
        loginParams.put("execution", executionValue);
        loginParams.put("lt", ltValue);
        loginParams.put("submit", "LOGIN");
        loginParams.put("_eventId", "submit");

        // �����¼����
        String res = HttpClientUtil.httpPost(seeConfig.getAddress() + CAS_LOGIN_URL, loginParams);

        if(SUCCESS.equals(res)) {
            // ҳ����ת����ȡcookie
            HttpClientUtil.httpGet(seeConfig.getAddress() + CAS_LOGIN_URL + "?service=http%3A%2F%2F10.20.36.109%3A8081%2Facm%2Fcloud.htm");
        } else {
            throw new Exception("�û���֤ʧ��");
        }
    }

    // ��ȡ��֤��Ϣ����
    public static String getAuth(SeeConfig seeConfig) throws URISyntaxException, IOException {
        String authJsonUrl = seeConfig.getAddress() + ACM_SYSTEM_AUTH_JSON_URL;
        return extractValueFromResponse(HttpClientUtil.httpPost(authJsonUrl, new HashMap<>()), "\"token\":\"(.*?)\"");
    }

    // ����Ӧ����ȡָ��ģʽ��ֵ
    public static String extractValueFromResponse(String response, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);
        return matcher.find() ? matcher.group(1) : "";
    }

    // ��ȡӦ����Ϣ����
    public static String getApplication(SeeConfig seeConfig, String applicationName, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        body.put("pageNo", "1");
        body.put("pageSize", "1");
        body.put("name", applicationName);
        body.put("allowedUpgradeMark", "true");
        header.put("Authorization", "Bearer " + auth);

        // �����ȡӦ����Ϣ������
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_DSSP_APPLICATION_QUERY_JSON_URL, body, header);
        JSONObject parse = JSONObject.parse(response);

        // ������Ӧ
        String errorInfo = parse.getString("error_info");
        if (StringUtils.isBlank(errorInfo)) {
            JSONArray jsonArray = parse.getJSONObject("data").getJSONArray("items");
            if (!jsonArray.isEmpty()) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                return jsonObject.getString("id");
            }
        } else {
            // ����������Ϣ
            handleError(errorInfo);
        }
        return "";
    }

    // ��ȡ�����ļ���Ϣ
    public static JSONObject getConfigInfo(SeeConfig seeConfig, String applicationId, String auth) throws IOException {
        Map<String, String> body = new HashMap<>();
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + auth);
        body.put("applicationId", applicationId);

        // �����ȡ������Ϣ������
        String response = HttpClientUtil.httpPost(seeConfig.getAddress() + ACM_DSSP_CONFIG_GET_COMPARE_CONFIG_JSON_URL, body, header);
        return JSONObject.parse(response);
    }

    // ����������Ϣ����
    private static   void handleError(String errorInfo) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showErrorDialog("error: " + errorInfo, "����");
        });
        throw new RuntimeException(errorInfo);
    }
}