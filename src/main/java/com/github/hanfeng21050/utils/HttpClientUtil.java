/*
 * 系统名称: 业务集中运营平台
 * 模块名称:
 * 类 名 称: HttpClientUtil.java
 * 软件版权: 杭州恒生电子股份有限公司
 * 相关文档:
 * 修改记录:
 * 修改日期 修改人员 修改说明<BR>
 * ======== ====== ============================================
 * ======== ====== ============================================
 * 评审记录：
 * 评审人员：
 * 评审日期：
 * 发现问题：
 */

package com.github.hanfeng21050.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * http客户端工具类
 * httpclient
 *
 * @author niusw40398
 * @date 2022/12/26
 */
public class HttpClientUtil {

    private final static Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    /**
     * httpclient连接池
     */
    private static PoolingHttpClientConnectionManager manager = null;

    /**
     * http客户端
     */
    private static volatile CloseableHttpClient httpClient = null;

    /**
     * 默认请求配置
     */
    private static RequestConfig defaultRequestConfig = null;

    private static CookieStore cookieStore = new BasicCookieStore();

    /**
     * 初始化
     */
    private static void init() {
        // 默认请求配置
        defaultRequestConfig = RequestConfig.custom()
                // HttpClient中的要用连接时尝试从连接池中获取，若是在等待了一定的时间后还没有获取到可用连接（比如连接池中没有空闲连接了）则会抛出获取连接超时异常。
                .setConnectTimeout(2000)
                // 指的是连接目标url的连接超时时间，即客服端发送请求到与目标url建立起连接的最大时间。如果在该时间范围内还没有建立起连接，则就抛出connectionTimeOut异常。
                .setSocketTimeout(3 * 60 * 1000)
                // 连接上一个url后，获取response的返回等待时间 ，即在与目标url建立连接后，等待放回response的最大时间，在规定时间内没有返回响应的话就抛出SocketTimeout。
                .setConnectionRequestTimeout(60 * 1000)
                .build();

        // httpclietn 池管理器
        SSLConnectionSocketFactory scsf = null;
        try {
            /// 解决httpClient发送https错误的问题
            scsf = new SSLConnectionSocketFactory(
                    SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                    NoopHostnameVerifier.INSTANCE);

            // 注册访问协议相关的 socket 工厂
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", scsf).build();
            // HttpConnection工产：配置写请求/解析相应处理器
            HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(DefaultHttpRequestWriterFactory.INSTANCE,
                    DefaultHttpResponseParserFactory.INSTANCE);
            // DNS 解析器
            DnsResolver dnsResolver = SystemDefaultDnsResolver.INSTANCE;
            // 创建池化连接管理器
            manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);
            // 默认为Socket 配置
            SocketConfig defaultSocketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
            manager.setDefaultSocketConfig(defaultSocketConfig);
            // 设置整个连接池的最大连接数 默认20
            manager.setMaxTotal(200);
            // 每个路由最大连接数 默认2
            manager.setDefaultMaxPerRoute(20);
            // 在从连接池获取连接时，连接不活跃多长时间后需要进行一次验证，默认为 2s
            manager.setValidateAfterInactivity(5 * 1000);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 得到http客户端
     *
     * @return {@link CloseableHttpClient}
     */
    public static synchronized CloseableHttpClient getHttpClient() {
        if (manager == null) {
            init();
        }
        // 双检锁, 保证只有一个实例
        if (httpClient == null) {
            synchronized (HttpClientUtil.class) {
                if (httpClient == null) {
                    httpClient = HttpClients.custom().setConnectionManager(manager)
                            // 连接池不是共享模式
                            .setConnectionManagerShared(false)
                            // 定期回收过期连接
                            .evictExpiredConnections()
                            // 连接存活时间，如果不设置，则根据长连接信息决定
                            .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                            // 设置默认请求配置
                            .setDefaultRequestConfig(defaultRequestConfig)
                            // 连接重用策略
                            .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                            // 长连接配置，即获得长连接生产多长时间
                            .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                            .setRetryHandler(DefaultHttpRequestRetryHandler.INSTANCE)
                            .setDefaultCookieStore(cookieStore)
                            .build();
                }
            }

        }

        // JVM停止或重启时 关闭连接池释放掉连接
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
        });

        return httpClient;
    }

    /**
     * 设置请求头
     *
     * @param httpRequestBase 请求
     * @param header          请求头
     */
    private static void setRequestHeader(HttpRequestBase httpRequestBase, Map<String, String> header) {
        if (header != null && header.size() > 0) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                httpRequestBase.addHeader(key, value);
            }
        }
    }

    /**
     * post请求
     *
     * @param url    请求地址
     * @param body   请求体
     * @param header 请求头
     * @return {@link String}
     */
    public static String httpPost(String url, Map<String, String> body, Map<String, String> header) throws IOException {
        List<NameValuePair> valuePairs = new LinkedList<NameValuePair>();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            valuePairs.add(new BasicNameValuePair(key, value));
        }
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(valuePairs, Consts.UTF_8));
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }

    /**
     * post请求
     *
     * @param url    请求地址
     * @param body   请求体
     * @param header 请求头
     * @return {@link String}
     */
    public static String httpPost(String url, String body, Map<String, String> header) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body, "UTF-8"));
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * post请求
     *
     * @param url        请求地址
     * @param httpEntity 请求体
     * @param header     请求头
     * @return {@link String}
     * @throws IOException ioexception
     */
    public static String httpPost(String url, HttpEntity httpEntity, Map<String, String> header) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(httpEntity);
        setRequestHeader(httpPost, header);
        CloseableHttpResponse response = null;
        try {
            response = request(httpPost);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * http post
     *
     * @param url  请求地址
     * @param body 请求体
     * @return {@link String}
     */
    public static String httpPost(String url, String body) throws IOException {
        return httpPost(url, body, null);
    }

    /**
     * post请求
     *
     * @param url  请求地址
     * @param body 请求体
     * @return {@link String}
     */
    public static String httpPost(String url, Map<String, String> body) throws IOException {
        return httpPost(url, body, null);
    }

    /**
     * post请求
     *
     * @param url        请求地址
     * @param httpEntity 请求体
     * @return {@link String}
     * @throws IOException ioexception
     */
    public static String httpPost(String url, HttpEntity httpEntity) throws IOException {
        return httpPost(url, httpEntity, null);
    }


    /**
     * http get请求
     *
     * @param url 请求地址
     * @return {@link String}
     */
    public static String httpGet(String url) throws URISyntaxException, IOException {
        return httpGet(url, null, null);
    }

    /**
     * http get请求
     *
     * @param url    请求地址
     * @param params 请求参数
     * @return {@link String}
     */
    public static String httpGet(String url, Map<String, String> params) throws URISyntaxException, IOException {
        return httpGet(url, params, null);
    }

    /**
     * http get请求
     *
     * @param url    请求地址
     * @param params 请求参数
     * @param header 请求头
     * @return {@link String}
     */
    public static String httpGet(String url, Map<String, String> params, Map<String, String> header) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(url);
        // 设置请求参数
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.getParams().setParameter("http.protocol.allow-circular-redirects", true);
        // 设置请求头
        setRequestHeader(httpGet, header);

        CloseableHttpResponse response = null;
        try {
            response = request(httpGet);
            return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
        } finally {
            if (null != response) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }


    /**
     * post
     *
     * @param httpRequest http post请求
     * @return {@link String}
     */
    public static CloseableHttpResponse request(HttpRequestBase httpRequest) throws IOException {
        CloseableHttpClient closeableHttpClient = getHttpClient();
        return closeableHttpClient.execute(httpRequest);
    }

    /**
     * 清除cookie
     */
    public static void clearCookie() {
        cookieStore.clear();
    }
}
