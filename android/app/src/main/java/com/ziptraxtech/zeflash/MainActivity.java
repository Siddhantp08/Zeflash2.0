package com.ziptraxtech.zeflash;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Bridge;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends BridgeActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get WebView reference
        WebView webView = this.bridge.getWebView();
        
        // Add JavaScript interface for app detection
        webView.addJavascriptInterface(new UpiAppInterface(), "AndroidUPIBridge");
        
        // Configure WebView for Razorpay UPI support
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Handle UPI and intent:// URLs
                if (url.startsWith("upi://") || url.startsWith("intent://")) {
                    try {
                        Intent intent;
                        if (url.startsWith("intent://")) {
                            intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                        }
                        
                        // Try to launch the intent
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                            return true;
                        }
                        
                        // If intent scheme, try fallback URL
                        if (url.startsWith("intent://")) {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            if (fallbackUrl != null) {
                                view.loadUrl(fallbackUrl);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                
                // Handle other external URLs (tez://, phonepe://, paytm://, etc.)
                if (url.startsWith("tez://") || 
                    url.startsWith("phonepe://") || 
                    url.startsWith("paytmmp://") ||
                    url.startsWith("gpay://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                
                // For all other URLs, let WebView handle it
                return super.shouldOverrideUrlLoading(view, request);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Inject detection script for UPI apps
                injectUpiDetectionScript(view);
            }
        });
        
        // Set WebChromeClient for popups and alerts
        webView.setWebChromeClient(new WebChromeClient());
        
        // Enable necessary WebView settings
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setUserAgentString(webView.getSettings().getUserAgentString() + " RazorpayUPI/1.0");
    }
    
    private void injectUpiDetectionScript(WebView webView) {
        String script = 
            "(function() {" +
            "  if (window.AndroidUPIBridge) {" +
            "    var installedApps = JSON.parse(window.AndroidUPIBridge.getInstalledUpiApps());" +
            "    window.INSTALLED_UPI_APPS = installedApps;" +
            "  }" +
            "})();";
        webView.evaluateJavascript(script, null);
    }
    
    public class UpiAppInterface {
        @JavascriptInterface
        public String getInstalledUpiApps() {
            try {
                JSONArray apps = new JSONArray();
                PackageManager pm = getPackageManager();
                
                // Create UPI intent
                Intent upiIntent = new Intent(Intent.ACTION_VIEW);
                upiIntent.setData(Uri.parse("upi://pay"));
                
                List<ResolveInfo> upiApps = pm.queryIntentActivities(upiIntent, 0);
                
                for (ResolveInfo app : upiApps) {
                    JSONObject appInfo = new JSONObject();
                    appInfo.put("packageName", app.activityInfo.packageName);
                    appInfo.put("appName", app.loadLabel(pm).toString());
                    apps.put(appInfo);
                }
                
                return apps.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }
        
        @JavascriptInterface
        public boolean isAppInstalled(String packageName) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
