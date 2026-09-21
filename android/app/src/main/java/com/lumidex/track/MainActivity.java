package com.lumidex.track;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import java.io.*;

public class MainActivity extends Activity {
    private WebView web;
    private static final String ORIGIN = "https://app.lumidex.local/";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        hideSystemBars();
        web = new WebView(this);
        setContentView(web);
        if (Build.VERSION.SDK_INT >= 30) {
            web.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "LumiAndroid");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith(ORIGIN)) return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                String path = request.getUrl().getPath().substring(1);
                if (path.isEmpty()) path = "index.html";
                if (path.contains("..")) return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                String mime = path.endsWith(".js") ? "application/javascript" : path.endsWith(".json") ? "application/json" : path.endsWith(".webp") ? "image/webp" : path.endsWith(".svg") ? "image/svg+xml" : "text/html";
                try { return new WebResourceResponse(mime, "UTF-8", getAssets().open("www/" + path)); }
                catch (IOException e) { return new WebResourceResponse("text/plain", "UTF-8", 404, "Not found", null, new ByteArrayInputStream(new byte[0])); }
            }
        });
        web.loadUrl(ORIGIN);
    }
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(android.view.WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }
    @Override protected void onResume() {
        super.onResume();
        try {
            if (new org.json.JSONObject(getSharedPreferences("save", 0).getString("snapshot", "{}")).optBoolean("monitoring")) startMonitor();
        } catch (org.json.JSONException ignored) { }
    }
    public class Bridge {
        @JavascriptInterface public String status() { return getSharedPreferences("save", 0).getString("snapshot", "{}"); }
        @JavascriptInterface public void chooseFolder() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                startActivityForResult(intent, 1);
            });
        }
        @JavascriptInterface public void stopMonitoring() {
            stopService(new Intent(MainActivity.this, SaveMonitor.class));
        }
        @JavascriptInterface public void startMonitoring() { runOnUiThread(() -> startMonitor()); }
    }
    private void startMonitor() {
        if (getSharedPreferences("save", 0).getString("tree", "").isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 2);
        startForegroundService(new Intent(this, SaveMonitor.class));
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 1 || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            stopService(new Intent(this, SaveMonitor.class));
            // Keep the last validated snapshot until the newly selected save can be read.
            getSharedPreferences("save", 0).edit().putString("tree", uri.toString()).apply();
            startMonitor();
        } catch (SecurityException e) { android.widget.Toast.makeText(this, "This folder cannot grant persistent read access.", android.widget.Toast.LENGTH_LONG).show(); }
    }
    @Override public void onBackPressed() { web.evaluateJavascript("history.back()", null); }
    @Override protected void onDestroy() { web.removeJavascriptInterface("LumiAndroid"); web.destroy(); super.onDestroy(); }
}
