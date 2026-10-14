
package com.minhajapps.webapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
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
    private View errorLayout;

    private boolean pageLoaded = false;

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
        FrameLayout.LayoutParams progressParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );
        rootLayout.addView(progressBar, progressParams);

        createErrorScreen();

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

        // Follow the system theme where WebView supports it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.setForceDark(
                    WebSettings.FORCE_DARK_AUTO
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.setAlgorithmicDarkeningAllowed(true);
        }

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
                if (errorLayout != null) {
                    errorLayout.setVisibility(View.GONE);
                }

                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                progressBar.setVisibility(View.GONE);

                if (errorLayout != null) {
                    errorLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    showErrorScreen();
                }
            }

            @Override
            public boolean onRenderProcessGone(
                    WebView view,
                    RenderProcessGoneDetail detail
            ) {
                showErrorScreen();
                return true;
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
                }
            }
        });

        if (savedInstanceState != null) {
            if (webView.restoreState(savedInstanceState) == null) {
                webView.loadUrl(HOME_URL);
            }
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    private void createErrorScreen() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.WHITE);

        android.widget.LinearLayout content =
                new android.widget.LinearLayout(this);

        content.setOrientation(
                android.widget.LinearLayout.VERTICAL
        );
        content.setGravity(Gravity.CENTER);
        content.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("ইন্টারনেট সংযোগ পাওয়া যাচ্ছে না");
        title.setTextColor(Color.rgb(30, 30, 30));
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText(
                "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
        );
        message.setTextColor(Color.DKGRAY);
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 16, 0, 24);

        Button retryButton = new Button(this);
        retryButton.setText("আবার চেষ্টা করুন");
        retryButton.setOnClickListener(v -> {
            pageLoaded = false;
            container.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            webView.loadUrl(HOME_URL);
        });

        content.addView(title);
        content.addView(message);
        content.addView(retryButton);

        container.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        container.setVisibility(View.GONE);
        rootLayout.addView(container, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        errorLayout = container;
    }

    private void showErrorScreen() {
        progressBar.setVisibility(View.GONE);

        if (errorLayout != null) {
            errorLayout.setVisibility(View.VISIBLE);
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
