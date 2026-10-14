package com.minhajapps.webapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL =
            "https://minhajapps.github.io/";
    private static final String HOST = "minhajapps.github.io";

    private static final String UPDATE_INFO_URL =
            "https://raw.githubusercontent.com/minhajapps/Minhaj-Apps/main/app-update.json";

    private WebView webView;
    private ProgressBar progress;
    private FrameLayout root;
    private LinearLayout errorScreen;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean pageLoaded = false;
    private boolean mainFrameFailed = false;
    private boolean updateDialogShowing = false;

    private final Runnable timeout = () -> {
        if (!pageLoaded && webView != null) {
            showError("লোড হতে দেরি হচ্ছে। ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।");
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        super.onCreate(state);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        root.addView(webView, new FrameLayout.LayoutParams(
                -1, -1));

        progress = new ProgressBar(this);

        FrameLayout.LayoutParams pp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER);

        root.addView(progress, pp);

        buildErrorScreen();

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {
                return openUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, String url) {
                return openUrl(Uri.parse(url));
            }

            private boolean openUrl(Uri uri) {
                if ("https".equalsIgnoreCase(uri.getScheme())
                        && HOST.equalsIgnoreCase(uri.getHost())) {
                    return false;
                }

                String scheme = uri.getScheme();

                if ("https".equalsIgnoreCase(scheme)
                        || "http".equalsIgnoreCase(scheme)
                        || "tel".equalsIgnoreCase(scheme)
                        || "mailto".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(
                                Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                    }
                }

                return true;
            }

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    android.graphics.Bitmap favicon) {

                pageLoaded = false;
                mainFrameFailed = false;

                errorScreen.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);

                handler.removeCallbacks(timeout);
                handler.postDelayed(timeout, 20000);
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {

                if (mainFrameFailed) return;

                pageLoaded = true;
                handler.removeCallbacks(timeout);
                progress.setVisibility(View.GONE);
                errorScreen.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {
                    mainFrameFailed = true;
                    handler.removeCallbacks(timeout);

                    showError(
                            "ওয়েবসাইট লোড হয়নি। ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।");
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view,
                    int code,
                    String description,
                    String failingUrl) {

                if (android.os.Build.VERSION.SDK_INT < 23
                        && failingUrl != null
                        && failingUrl.equals(view.getUrl())) {

                    mainFrameFailed = true;
                    handler.removeCallbacks(timeout);
                    showError("ওয়েবসাইট লোড হয়নি। আবার চেষ্টা করুন।");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(
                    WebView view, int value) {

                if (!pageLoaded && !mainFrameFailed) {
                    progress.setVisibility(View.VISIBLE);
                }
            }
        });

        if (state != null) {
            if (webView.restoreState(state) == null) {
                loadHome();
            }
        } else {
            loadHome();
        }
    }

    private void checkForMandatoryUpdate() {
        Thread thread = new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        UPDATE_INFO_URL + "?t="
                                + System.currentTimeMillis());

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setUseCaches(false);

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return;
                }

                StringBuilder body = new StringBuilder();

                try (
                        InputStream input =
                                connection.getInputStream();

                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                input,
                                                StandardCharsets.UTF_8))
                ) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                JSONObject data =
                        new JSONObject(body.toString());

                int latestVersionCode =
                        data.optInt("versionCode", 0);

                String downloadUrl =
                        data.optString("downloadUrl", "").trim();

                String message = data.optString(
                        "message",
                        "নতুন ভার্সন ব্যবহার করতে এখনই আপডেট করুন।");

                int installedVersionCode =
                        getInstalledVersionCode();

                if (latestVersionCode > installedVersionCode
                        && downloadUrl.startsWith(
                                "https://www.mediafire.com/")) {

                    runOnUiThread(() ->
                            showMandatoryUpdateDialog(
                                    message, downloadUrl));
                }

            } catch (Exception ignored) {
                // নেটওয়ার্ক বা সার্ভার ব্যর্থ হলে বর্তমান অ্যাপ চালু থাকবে।
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private int getInstalledVersionCode() {
        try {
            PackageInfo info =
                    getPackageManager().getPackageInfo(
                            getPackageName(), 0);

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                return (int) info.getLongVersionCode();
            }

            //noinspection deprecation
            return info.versionCode;

        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void showMandatoryUpdateDialog(
            String message, String downloadUrl) {

        if (isFinishing()
                || isDestroyed()
                || updateDialogShowing) {
            return;
        }

        updateDialogShowing = true;

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("MINHAJ APPS আপডেট প্রয়োজন")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(
                                "আপডেট করুন",
                                (d, which) -> {
                                    try {
                                        startActivity(
                                                new Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(downloadUrl)));
                                    } catch (Exception ignored) {
                                    }
                                })
                        .create();

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnDismissListener(d ->
                updateDialogShowing = false);

        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
    }

    private void buildErrorScreen() {
        errorScreen = new LinearLayout(this);
        errorScreen.setOrientation(LinearLayout.VERTICAL);
        errorScreen.setGravity(Gravity.CENTER);
        errorScreen.setPadding(32, 32, 32, 32);
        errorScreen.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("MINHAJ APPS");
        title.setTextSize(23);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText(
                "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।");
        message.setTextSize(16);
        message.setTextColor(Color.DKGRAY);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 16, 0, 16);

        Button retry = new Button(this);
        retry.setText("আবার চেষ্টা করুন");
        retry.setOnClickListener(v -> loadHome());

        errorScreen.addView(title);
        errorScreen.addView(message);
        errorScreen.addView(retry);

        errorScreen.setVisibility(View.GONE);

        root.addView(errorScreen,
                new FrameLayout.LayoutParams(-1, -1));
    }

    private void showError(String message) {
        if (isFinishing() || webView == null) return;

        TextView text =
                (TextView) errorScreen.getChildAt(1);

        text.setText(message);
        progress.setVisibility(View.GONE);
        errorScreen.setVisibility(View.VISIBLE);
    }

    private void loadHome() {
        if (webView == null) return;

        handler.removeCallbacks(timeout);

        pageLoaded = false;
        mainFrameFailed = false;

        errorScreen.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        webView.stopLoading();
        webView.loadUrl(HOME_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForMandatoryUpdate();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        if (webView != null) {
            webView.saveState(out);
        }

        super.onSaveInstanceState(out);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (updateDialogShowing) {
            return;
        }

        if (errorScreen != null
                && errorScreen.getVisibility() == View.VISIBLE) {

            errorScreen.setVisibility(View.GONE);

            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                super.onBackPressed();
            }

            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);

            root.removeView(webView);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
