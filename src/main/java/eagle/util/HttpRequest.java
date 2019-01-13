package eagle.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eagle.dao.HttpClientResult;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

@Service
@Scope(value = "prototype")
public class HttpRequest {
    private String url;
    private String parameter = "";
    private String httpHeader = "";
    private String httpMethod = "";
    private String cookie = "";
    private CloseableHttpClient httpClient;
    private HttpClientConnectionManager connManager;
    private static final String ENCODING = "UTF-8";
    @Autowired
    JsonUtil jsonUtil;

    public HttpRequest() {
        // TODO Auto-generated constructor stub
    }

    private void init() {
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                // 正常的SSL连接会验证码所有证书信息
                // .register("https", new SSLConnectionSocketFactory(sslcontext)).build();
                // 只忽略域名验证码
                .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)).build();
        ConnectionKeepAliveStrategy kaStrategy = new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    //如果服务器没有设置keep-alive这个参数，我们就把它设置成1分钟
                    keepAlive = 60000;
                }
                return keepAlive;
            }
        };

        connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        httpClient = HttpClients.custom().setKeepAliveStrategy(kaStrategy).setConnectionManager(connManager).build();
    }
    private void configParameters(HttpEntityEnclosingRequestBase http_request) {
        StringEntity entity = null;
        setHttpHeader(http_request);
        if (!this.httpHeader.contains("x-www-form-urlencoded")) {
            entity = new StringEntity(this.parameter, "UTF-8");
        } else {
            List<NameValuePair> list = new ArrayList<>();
            JsonObject jsonObject = jsonUtil.getJson(this.parameter).getAsJsonObject();
            Set<Entry<String, JsonElement>> set = jsonObject.entrySet();
            for (Entry<String, JsonElement> entry : set) {
                String value = entry.getValue().toString();
                if (value.startsWith("\"")) {
                    list.add(new BasicNameValuePair(entry.getKey(), value.substring(1, value.length() - 1)));
                } else {
                    list.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            try {
                entity = new UrlEncodedFormEntity(list);
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        http_request.setEntity(entity);
    }

    public HttpClientResult getResponseByPostMethod() {
        init();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost();
            httpPost.reset();
            httpPost.setURI(new URI(url));
            configParameters(httpPost);
            response = httpClient.execute(httpPost);
            return  new HttpClientResult(response.getStatusLine().getStatusCode(),extractResponseResult(response));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            try {
                closeConnections(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void setHttpHeader(HttpRequestBase httpRequest) {
        HashMap<String, String> para_map = null;
        para_map = jsonUtil.getGson().fromJson(httpHeader, HashMap.class);
        if (para_map == null) {
            System.out.println("HttpHeader不存在或者不是json！");
        } else {
            for (String key : para_map.keySet()) {
                httpRequest.addHeader(key, para_map.get(key));
            }
        }
        httpRequest.addHeader("Cookie", cookie);
    }
    public static String extractResponseResult(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String result = "";
        if (entity != null) {
            result = EntityUtils.toString(entity,ENCODING);
        }
        return result;
    }
    private void  closeConnections(CloseableHttpResponse closeableHttpResponse) throws IOException {
        if(closeableHttpResponse!=null){
            EntityUtils.consume(closeableHttpResponse.getEntity());
            closeableHttpResponse.close();
        }

    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getHttpHeader() {
        return httpHeader;
    }

    public void setHttpHeader(String httpHearder) {
        this.httpHeader = httpHearder;
    }

    public String getHttpMethod() {
        return httpMethod;
    }
}
