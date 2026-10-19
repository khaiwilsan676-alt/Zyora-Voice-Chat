package com.hawa.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.webkit.WebView;

import androidx.activity.OnBackPressedCallback;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the real Hurry launch screen visible immediately after Android's
        // system splash, instead of showing a white WebView while the app starts.
        showNativeSplash();

        // Jitsi voice/video uses Android runtime permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), 4101);
            }
        }

        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }

        if (bridge != null && bridge.getWebView() != null) {
            android.webkit.WebSettings settings = bridge.getWebView().getSettings();
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);

            WebView webView = bridge.getWebView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                webView.setOnApplyWindowInsetsListener((view, insets) -> {
                    android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
                    );
                    view.setPadding(bars.left, 0, bars.right, bars.bottom);
                    view.post(() -> webView.evaluateJavascript(
                        "document.documentElement.style.setProperty('--status-bar-height','" + bars.top + "px')",
                        null
                    ));
                    return insets;
                });
                webView.requestApplyInsets();
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getOnBackPressedDispatcher().addCallback(
            this,
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (bridge != null && bridge.getWebView() != null
                            && bridge.getWebView().canGoBack()) {
                        bridge.getWebView().goBack();
                    }
                }
            }
        );
    }

    private void showNativeSplash() {
        ViewGroup root = findViewById(android.R.id.content);
        View splash = LayoutInflater.from(this).inflate(R.layout.native_splash, root, false);
        root.addView(splash, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        new Handler(getMainLooper()).postDelayed(() -> {
            if (splash.getParent() instanceof ViewGroup) {
                ((ViewGroup) splash.getParent()).removeView(splash);
            }
        }, 1200);
    }
}
