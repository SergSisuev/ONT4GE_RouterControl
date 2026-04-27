package com.example.routercontrol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RouterAction extends AsyncTask {
    private final CountDownLatch latch;
    private final Context context;
    private final String loginSuffiks = "/cgi-bin/login.cgi";
    private final String securitySuffiks = "/cgi-bin/macfilter.cgi";
    private final String indexSuffiks = "/cgi-bin/index.cgi";
    private String cookies;
    private String gcSessionKey;

    RouterAction(CountDownLatch latch, Context context) {
        this.context = context;
        this.latch = latch;
    }

    private void doRouterAuth(String mainUrl) throws Exception {
        String body = "";
        try {
            Log.d("doRouterAuth", "doRouterAuth are going to be executed");
            AppLogger.addLog(context, "SUCCESS", "doRouterAuth are going to be executed");
            HashMap<String, String> parameters = new HashMap<String, String>() {{
                put("onSubmit", "1");
                put("login_language", "English");
                put("encryPassword", getMD5HeshValue(RouterState.getPassword()));
                put("encryUsername", getMD5HeshValue(RouterState.getName()));
            }};
            body = postFormUrlEncodedRequest(mainUrl, parameters);
            Log.d("doRouterAuth", "doRouterAuth has been executed");
            AppLogger.addLog(context, "SUCCESS", "doRouterAuth has been executed");
        } catch (Exception e) {
            Log.e("doRouterAuth", "doRouterAuth error:", e);
            AppLogger.addLog(context, "FAILED", "doRouterAuth error:");
            RouterState.setOperationStatus(3);
        }
        if (cookies == null) {
            AppLogger.addLog(context, "FAILED", "doRouterAuth failed. Url:" + mainUrl);
            AppLogger.addLog(context, "FAILED", "doRouterAuth failed. Password:" + RouterState.getPassword());
            AppLogger.addLog(context, "FAILED", "doRouterAuth failed. Name:" + RouterState.getName());
            AppLogger.addLog(context, "FAILED", "doRouterAuth failed. Body:" + body);
            throw new Exception("Login failed. No login session found");
        }
    }

    private String doRouterLogout(String mainUrl) throws Exception {
        Log.d("doRouterLogout", "doRouterLogout are going to be executed");
        AppLogger.addLog(context, "SUCCESS", "doRouterLogout are going to be executed");
        try {
            HashMap<String, String> parameters = new HashMap<String, String>() {{
                put("onSubmit", "2");
                put("language_select", "English");
            }};
            String responseHtml = getRequestWithQuery(mainUrl, parameters);
            Log.d("doRouterLogout", "doRouterLogout has been executed");
            return responseHtml;
        } catch (Exception e) {
            Log.e("doRouterLogout", "doRouterLogout error:", e);
            AppLogger.addLog(context, "FAILED", "doRouterLogout error:");
            RouterState.setOperationStatus(3);
        }
        return "";
    }

    private String getSecurityPageWithSessionKey(String securityUrl) {
        Log.d("getSecurityPageWithSessionKey", "getRequestWithQuery are going to be executed");
        AppLogger.addLog(context, "SUCCESS", "getRequestWithQuery are going to be executed");
        try {
//            HashMap<String, String> parameters = new HashMap<String, String>() {{
//                put("onSubmit", "2");
//                put("pvceditindex", "1");
//                put("child_index", "1");
//                put("encapmode", "IPoE");
//                put("operate", "modify");
//            }};
            String responseHtml = getRequestWithQuery(securityUrl, null);
            Log.d("getSecurityPageWithSessionKey", "getSecurityPageWithSessionKey has been executed");
            gcSessionKey = extractVariableValue(responseHtml, "gcsessionkey");
            Log.d("getSecurityPageWithSessionKey", "gcSessionKey value: " + gcSessionKey);
            AppLogger.addLog(context, "SUCCESS", "gcSessionKey value: " + gcSessionKey);
            return responseHtml;
        } catch (Exception e) {
            Log.e("getWanPageWithSessionKey", "getWanPageWithSessionKey error:", e);
            AppLogger.addLog(context, "FAILED", "getWanPageWithSessionKey error:");
            RouterState.setOperationStatus(3);
        }
        return "";
    }

    private void changeMacFilter(String securityUrl, int filterEnableValue) {
        Log.d("changeMacFilter", "changeMacFilter request are going to be executed");
        AppLogger.addLog(context, "SUCCESS", "changeMacFilter request are going to be executed");
        try {
            HashMap<String, String> parameters = new HashMap<String, String>() {{
                put("onSubmit", "1");
                put("sessionkey", gcSessionKey);
                put("operation", "page_submit");
                put("delnumberlist", "");
                put("filter_enable", String.valueOf(filterEnableValue));
                put("modify_index", "");
                put("src_macaddress", "");
                put("dst_macaddress", "");
                put("macfilter_enable", "on");
                put("macfilter_mode", "Exclude");
                put("macfilter_protocol", "IP");
                put("macfilter_scmac", "");
                put("macfilter_dsmac", "");
            }};
            String stopWanHtml = postFormUrlEncodedRequest(securityUrl, parameters);
            Log.d("changeMacFilter", "changeMacFilter request has been executed");
            AppLogger.addLog(context, "SUCCESS", "changeMacFilter request has been executed");
//            Log.d("doAuthPostRequest", "stop wan response:\n" + stopWanHtml);
        } catch (Exception e) {
            Log.e("changeMacFilter", "changeMacFilter error:", e);
            AppLogger.addLog(context, "FAILED", "changeMacFilter error:");
            RouterState.setOperationStatus(3);
        }
    }

    private String postFormUrlEncodedRequest(String url, HashMap<String, String> parameters) {
        try {
            // Create request
            URL reqUrl = new URL(url);
            HttpURLConnection request = (HttpURLConnection) reqUrl.openConnection();
            request.setDoOutput(true);
            request.setDoInput(true);
            request.setRequestMethod("POST");
            request.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            request.setRequestProperty("upgrade-insecure-requests", "1");
            request.setRequestProperty("dnt", "1");
            request.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36");
            request.setUseCaches(false);
            if (cookies != null)
                request.setRequestProperty("Cookie", cookies);
            //Create POST parameter string
            String urlParameters = getDataString(parameters);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            request.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            DataOutputStream os = new DataOutputStream(request.getOutputStream());
            os.write(postData);
            os.flush();
            os.close();

            // Process response
            StringBuilder responseBody = getResponseBody(request);
//            Log.d("postFormUrlEncodedRequest","ResponseBody: " + responseBody);
            processCookies(request.getHeaderFields().get("Set-Cookie"));
            return responseBody.toString();
        } catch (Exception e) {
            Log.e("postFormUrlEncodedRequest", "postFormUrlEncodedRequest error:", e);
        }
        return "";
    }

    private String getRequestWithQuery(String url, HashMap<String, String> parameters) {
        try {
            // Create request
            URL reqUrl = new URL(url + "?" + getDataString(parameters));
            Log.d("getRequestWithQuery", "GET request query: " + reqUrl.getQuery());
            HttpURLConnection request = (HttpURLConnection) reqUrl.openConnection();
            request.setDoOutput(true);
            request.setDoInput(true);
            request.setRequestMethod("GET");
            request.setRequestProperty("upgrade-insecure-requests", "1");
            request.setRequestProperty("dnt", "1");
            request.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36");
            request.setUseCaches(false);
            if (cookies != null)
                request.setRequestProperty("Cookie", cookies);
            //Create POST parameter string
            request.connect();

            // Process response
            StringBuilder responseBody = getResponseBody(request);
//            Log.d("postFormUrlEncodedRequest","ResponseBody: " + responseBody);
            processCookies(request.getHeaderFields().get("Set-Cookie"));
            return responseBody.toString();
        } catch (Exception e) {
            Log.e("getRequestWithQuery", "getRequestWithQuery error:", e);
        }
        return "";
    }

    @NonNull
    private StringBuilder getResponseBody(HttpURLConnection request) throws IOException {
        BufferedReader br;
        StringBuilder responseBody = new StringBuilder();
        try {
            if (100 <= request.getResponseCode() && request.getResponseCode() <= 399) {
                br = new BufferedReader(new InputStreamReader(request.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(request.getErrorStream()));
            }
            String line;
            while ((line = br.readLine()) != null) {
                responseBody.append(line).append("\n");
            }
        } catch (Exception e) {
            Log.e("getResponseBody", "getResponseBody error: " + e.getMessage());
            AppLogger.addLog(context, "FAILED", "getResponseBody error: " + e.getMessage());
        }
        return responseBody;
    }

    private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        if (params == null) return "";
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    private void processCookies(List<String> cookiesList) {
        if (cookiesList != null && cookiesList.size() >= 2) {
            cookies = cookiesList.get(0)
                    .replace(";", "")
                    .replace("PATH=/", "")
                    .replace("HttpOnly", "").trim()
                    + ";" + cookiesList.get(1)
                    .replace(";", "")
                    .replace("PATH=/", "")
                    .replace("HttpOnly", "").trim();
            Log.d("doInBackground", "cookies: " + cookies);
        } else {
            Log.d("doInBackground", "No cookies found");
        }
    }

    private String extractVariableValue(String html, String varName) {
        if (html == null || varName == null) return "";
        String regex = "var\\s+" + varName + "\\s*=\\s*[\"']([^\"']+)[\"']";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getMD5HeshValue(String stringValue) throws NoSuchAlgorithmException {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(stringValue.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1,digest);
        return bigInt.toString(16);
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            Log.d("doInBackground", "doInBackground. Parameters: " + Arrays.toString(objects));
            int filterEnableCommand = (Integer) objects[0];
            boolean onlyCheck = (Boolean) objects[1];
            Log.d("doInBackground", "doInBackground. filterEnableCommand: " + filterEnableCommand);
            AppLogger.addLog(context, "SUCCESS", "doInBackground. filterEnableCommand: " + filterEnableCommand);
            // Login and create session
            doRouterAuth(RouterState.getMainHttpAddress() + loginSuffiks);
            // Get security page and get gcSessionKey
            String securityHtml = getSecurityPageWithSessionKey(RouterState.getMainHttpAddress() + securitySuffiks);
            if (gcSessionKey == null || gcSessionKey.isEmpty()) {
                throw new Exception("Get Security Page request failed. No security session found");
            }
            int isEnabled = Integer.parseInt(extractVariableValue(securityHtml, "macfilter_enable"));
            Log.d("doInBackground", "doInBackground. Current security filter enabled: " + isEnabled);
            AppLogger.addLog(context, "SUCCESS", "doInBackground. Current security filter enabled: " + isEnabled);
            if (onlyCheck || isEnabled == filterEnableCommand) {
                RouterState.setRestrictionApplied(isEnabled == 1);
            } else {
                // Enable or disable white list filter
                changeMacFilter(RouterState.getMainHttpAddress() + securitySuffiks, filterEnableCommand);
            }
            RouterState.setOperationStatus(2);
            latch.countDown();
            // Do the logout to finish session
            doRouterLogout(RouterState.getMainHttpAddress() + indexSuffiks);
        } catch (Exception e) {
            Log.e("doInBackground", "doInBackground error:", e);
            AppLogger.addLog(context, "FAILED", "doInBackground. Task execution failed: " + e.getMessage());
            RouterState.setOperationStatus(3);
        }
        return null;
    }
}
