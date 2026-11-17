package com.aitraining.sim;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private BrainEngine brain;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {
            brain = new BrainEngine(getAssets());
        } catch (Exception e) {
            Log.e("MyFirstAI", "BANC brain asset could not be loaded", e);
        }

        WebView w = new WebView(this);
        w.setWebViewClient(new WebViewClient());
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.addJavascriptInterface(new NativeBrainBridge(), "NativeBrain");
        w.setBackgroundColor(0xff080b14);
        setContentView(w);
        w.loadUrl("file:///android_asset/index.html");
    }

    private final class NativeBrainBridge {
        @JavascriptInterface public String info() {
            return brain == null ? "{\"error\":\"BANC asset missing\"}" : brain.infoJson();
        }
        @JavascriptInterface public String step(float touch, float gait, float obstacle) {
            return brain == null ? "{\"error\":\"BANC asset missing\"}" : brain.stepJson(touch, gait, obstacle);
        }
        @JavascriptInterface public String status() {
            return brain == null ? "{\"error\":\"BANC asset missing\"}" : brain.statusJson();
        }
    }

    @Override public void onBackPressed() { super.onBackPressed(); }
}
