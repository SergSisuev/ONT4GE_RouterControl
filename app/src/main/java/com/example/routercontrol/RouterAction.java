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
    private final String wanSuffiks = "/cgi-bin/wanpon_edit.cgi";
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

    private String getWanPageWithSessionKey(String wanUrl) {
        Log.d("getWanPageWithSessionKey", "getRequestWithQuery are going to be executed");
        AppLogger.addLog(context, "SUCCESS", "getRequestWithQuery are going to be executed");
        try {
            HashMap<String, String> parameters = new HashMap<String, String>() {{
                put("onSubmit", "2");
                put("pvceditindex", "1");
                put("child_index", "1");
                put("encapmode", "IPoE");
                put("operate", "modify");
            }};
            String wanponHtml = getRequestWithQuery(wanUrl, parameters);
            Log.d("getWanPageWithSessionKey", "getRequestWithQuery has been executed");
            gcSessionKey = extractVariableValue(wanponHtml, "gcsessionkey");
            Log.d("getWanPageWithSessionKey", "gcSessionKey value: " + gcSessionKey);
            AppLogger.addLog(context, "SUCCESS", "gcSessionKey value: " + gcSessionKey);
            return wanponHtml;
        } catch (Exception e) {
            Log.e("getWanPageWithSessionKey", "getWanPageWithSessionKey error:", e);
            AppLogger.addLog(context, "FAILED", "getWanPageWithSessionKey error:");
            RouterState.setOperationStatus(3);
        }
        return "";
    }

    private void changeWanConnectionNat(int natParameterValue, String wanponHtml, String wanUrl) {
        Log.d("changeWanConnectionNat", "changeWanConnectionNat request are going to be executed");
        AppLogger.addLog(context, "SUCCESS", "changeWanConnectionNat request are going to be executed");
        try {
            HashMap<String, String> parameters = new HashMap<String, String>() {{
                put("onSubmit", "1");
                put("pvcindex", extractVariableValue(wanponHtml, "pvcindex"));
                put("child_index", extractVariableValue(wanponHtml, "child_index"));
                put("pvcenable", "1");
                put("pvcdhcpenable", "1");
                put("encapmode", extractVariableValue(wanponHtml, "encapsulatemode"));
                put("pvcipprotocol", "1");
                put("pppauthtype", "");
                put("pppconntrigger", "");
                put("ipacqmode", "Static");
                put("ipnat", String.valueOf(natParameterValue));
                put("sessionkey", gcSessionKey);
                put("ipv6getmodeid", "");
                put("ipv6staticguaid", "");
                put("ipv6staticguagwid", "");
                put("ipv6staticdnsid", "");
                put("prefixdelegateid", "");
                put("X_8021pvalue", "0");
                put("wanpon_operation", "modify_pvc");
                put("dslite_enable", "0");
                put("laninterface", "");
                put("nptv6_enable", "0");
                put("wanpon_connectionType", "IP_Routed");
                put("dscp_enable", "0");
                put("pppoe_proxyenable", "");
                put("passthrough_enable", "");
                put("servicetype", extractVariableValue(wanponHtml, "servicetype"));
                put("wanponedit_ppppassword", "");
                put("wanpon_connectname", "1_1_IPoE");
                put("wanponedit_vlanmode", "2");
                put("wanponedit_vlanid", "903");
                put("wanponedit_8021p", "0");
                put("wanponedit_ppptranstype", "PPPoE");
                put("wanponedit_pppusername", "");
                put("wanponedit_dmsname", "");
                put("wanponedit_authtype", "Auto");
                put("wanponedit_ppptrigger", "AlwaysOn");
                put("wanponedit_idletime", "");
                put("wanponedit_Ipv4getmode", "Static");
                put("wanponedit_staticip", "10.14.31.243");
                put("wanponedit_staticmask", "255.255.252.0");
                put("wanponedit_staticgateway", "10.14.31.254");
                put("wanponedit_staticdns1", "192.168.192.168");
                put("wanponedit_staticdns2", "192.168.168.192");
                put("wanponedit_staticdns3", "");
                put("ipv6getmode", "Manual");
                put("wanponedit_ipv6staticgua", "None");
                put("wanponedit_ipv6staticguaipaddress", "");
                put("wanponedit_ipv6staticguaiplen", "");
                put("wanponedit_ipv6staticguaipgwaddress", "");
                put("wanponedit_ipv6staticdns1", "");
                put("wanponedit_ipv6staticdns2", "");
                put("wanponedit_ipv6staticdns3", "");
                put("wanponedit_ipv6staticpdipprefix", "");
                put("wanponedit_ipv6staticpdiplen", "");
                put("wanponedit_nat", "on");
                put("wanponedit_mtu", "1500");
                put("wanponedit_multicastvlan", "");
            }};
            String stopWanHtml = postFormUrlEncodedRequest(wanUrl, parameters);
            Log.d("changeWanConnectionNat", "changeWanConnectionNat request has been executed");
            AppLogger.addLog(context, "SUCCESS", "changeWanConnectionNat request has been executed");
//            Log.d("doAuthPostRequest", "stop wan response:\n" + stopWanHtml);
        } catch (Exception e) {
            Log.e("changeWanConnectionNat", "changeWanConnectionNat error:", e);
            AppLogger.addLog(context, "FAILED", "changeWanConnectionNat error:");
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

    private void bindToWifi(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

                connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        // Привязываем все запросы приложения к этой сети
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            connectivityManager.bindProcessToNetwork(network);
                            Log.d("bindToWifi", "The application bound to WiFi only");
                        }
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);
                        // Освобождаем привязку, если Wi-Fi пропал
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            connectivityManager.bindProcessToNetwork(null);
                            Log.d("WiFi", "Wi-Fi lost, the binding is canceled");
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e("bindToWifi", "bindToWifi. Binding failed.", e);
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            Log.d("doInBackground", "doInBackground. Parameters: " + Arrays.toString(objects));
            int changeNatCommand = (Integer) objects[0];
            boolean onlyCheck = (Boolean) objects[1];
            Log.d("doInBackground", "doInBackground. changeNatCommand: " + changeNatCommand);
            AppLogger.addLog(context, "SUCCESS", "doInBackground. changeNatCommand: " + changeNatCommand);
            // Bind to Wi-Fi only
            bindToWifi(context);
            // Login and create session
            doRouterAuth(RouterState.getMainHttpAddress() + loginSuffiks);
            // Get wanpon page and get gcSessionKey
            String wanponHtml = getWanPageWithSessionKey(RouterState.getMainHttpAddress() + wanSuffiks);
            if (gcSessionKey == null || gcSessionKey.isEmpty()) {
                throw new Exception("Get Wan Page request failed. No WAN session found");
            }
            int nat = Integer.parseInt(extractVariableValue(wanponHtml, "nat"));
            Log.d("doInBackground", "doInBackground. Check current WEB state: " + nat);
            AppLogger.addLog(context, "SUCCESS", "doInBackground. Check current WEB state: " + nat);
            if (onlyCheck || changeNatCommand == nat) {
                RouterState.setRestrictionApplied(nat == 0);
                RouterState.setOperationStatus(2);
                latch.countDown();
                return null;
            }
            // Change WAN connection NAT parameter
            changeWanConnectionNat(changeNatCommand, wanponHtml, RouterState.getMainHttpAddress() + wanSuffiks);
            RouterState.setOperationStatus(2);
            latch.countDown();
        } catch (Exception e) {
            Log.e("doInBackground", "doInBackground error:", e);
            AppLogger.addLog(context, "FAILED", "doInBackground. Task execution failed: " + e.getMessage());
            RouterState.setOperationStatus(3);
        }
        return null;
    }
}
