
package com.minhajapps.webapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL =
            "https://minhajapps.github.io/";

    private static final String ALLOWED_HOST =
            "minhajapps.github.io";

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout rootLayout;
    private LinearLayout errorLayout;
    private TextView errorMessage;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean mainPageLoaded = false;

    private final Runnable loadingTimeout = () -> {
        if (!mainPageLoaded && webView != null) {
            showError("ওয়েবসাইট লোড হতে দেরি হচ্ছে। "
                    + "ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );

        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);
        setContentView(rootLayout);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        rootLayout.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);

        FrameLayout.LayoutParams progressParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );

        rootLayout.addView(progressBar, progressParams);

        createErrorLayout();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(
                webView, true
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                return handleUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(Uri uri) {
                String scheme = uri.getScheme();
                String host = uri.getHost();

                if ("https".equalsIgnoreCase(scheme)
                        && ALLOWED_HOST.equalsIgnoreCase(host)) {
                    return false;
                }

                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)
                        || "mailto".equalsIgnoreCase(scheme)
                        || "tel".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(
                                Intent.ACTION_VIEW, uri
                        ));
                    } catch (Exception ignored) {
                    }
                }

                return true;
            }

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    android.graphics.Bitmap favicon
            ) {
                mainPageLoaded = false;
                errorLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                handler.removeCallbacks(loadingTimeout);
                handler.postDelayed(loadingTimeout, 20000);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                mainPageLoaded = true;
                handler.removeCallbacks(loadingTimeout);
                progressBar.setVisibility(View.GONE);
                errorLayout.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    handler.removeCallbacks(loadingTimeout);
                    showError(
                            "ওয়েবসাইট লোড হয়নি। "
                            + "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
                    );
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl
            ) {
                if (android.os.Build.VERSION.SDK_INT < 23) {
                    if (failingUrl != null
                            && failingUrl.equals(view.getUrl())) {
                        handler.removeCallbacks(loadingTimeout);
                        showError(
                                "ওয়েবসাইট লোড হয়নি। "
                                + "ইন্টারনেট পরীক্ষা করে আবার চেষ্টা করুন।"
                        );
                    }
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(
                    WebView view,
                    int newProgress
            ) {
                if (newProgress >= 100 && mainPageLoaded) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        if (savedInstanceState != null) {
            if (webView.restoreState(savedInstanceState) == null) {
                loadHome();
            }
        } else {
            loadHome();
        }
    }

    private void createErrorLayout() {
        errorLayout = new LinearLayout(this);
        errorLayout.setOrientation(LinearLayout.VERTICAL);
        errorLayout.setGravity(Gravity.CENTER);
        errorLayout.setPadding(32, 32, 32, 32);
        errorLayout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("MINHAJ APPS");
        title.setTextColor(Color.rgb(35, 35, 35));
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        errorMessage = new TextView(this);
        errorMessage.setText(
                "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
        );
        errorMessage.setTextColor(Color.DKGRAY);
        errorMessage.setTextSize(16);
        errorMessage.setGravity(Gravity.CENTER);
        errorMessage.setPadding(0, 20, 0, 20);

        Button retryButton = new Button(this);
        retryButton.setText("আবার চেষ্টা করুন");
        retryButton.setOnClickListener(v -> loadHome());

        errorLayout.addView(title);
        errorLayout.addView(errorMessage);
        errorLayout.addView(retryButton);

        errorLayout.setVisibility(View.GONE);

        rootLayout.addView(errorLayout,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
    }

    private void showError(String message) {
        if (isFinishing() || webView == null) {
            return;
        }

        progressBar.setVisibility(View.GONE);
        errorMessage.setText(message);
        errorLayout.setVisibility(View.VISIBLE);
    }

    private void loadHome() {
        if (webView == null) {
            return;
        }

        handler.removeCallbacks(loadingTimeout);
        mainPageLoaded = false;
        errorLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        webView.stopLoading();
        webView.loadUrl(HOME_URL);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (errorLayout != null
                && errorLayout.getVisibility() == View.VISIBLE) {
            if (webView != null && webView.canGoBack()) {
                errorLayout.setVisibility(View.GONE);
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

            if (rootLayout != null) {
                rootLayout.removeView(webView);
            }

            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
