package com.void.aitraining;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.view.Window;
import android.view.WindowManager;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b); requestWindowFeature(Window.FEATURE_NO_TITLE);
  getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
  WebView w=new WebView(this); w.setWebViewClient(new WebViewClient());
  WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
  w.setBackgroundColor(0xff080b14); setContentView(w);
  w.loadUrl("file:///android_asset/index.html");
 }
 @Override public void onBackPressed() { super.onBackPressed(); }
}
