package com.minhajapps.webapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private FrameLayout rootLayout;

    private static final String HOME_URL =
            "https://minhajapps.github.io/";

    private static final String ALLOWED_HOST =
            "minhajapps.github.io";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );

        super.onCreate(savedInstanceState);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);

        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        rootLayout.addView(webView);

        progressBar = new ProgressBar(this);

        FrameLayout.LayoutParams progressParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );

        progressBar.setLayoutParams(progressParams);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        rootLayout.addView(progressBar);

        setContentView(rootLayout);

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
        CookieManager.getInstance()
                .setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                return openUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {
                return openUrl(Uri.parse(url));
            }

            private boolean openUrl(Uri uri) {
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
                        Intent intent = new Intent(
                                Intent.ACTION_VIEW, uri
                        );
                        startActivity(intent);
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
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(
                    WebView view,
                    int newProgress
            ) {
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME_URL);
        }
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
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
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
