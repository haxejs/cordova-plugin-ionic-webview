package com.ionicframework.cordova.webview;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;

import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;
import org.apache.cordova.engine.SystemWebViewClient;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.apache.cordova.engine.SystemWebView;

public class IonicWebViewEngine extends SystemWebViewEngine {
  public static final String TAG = "IonicWebViewEngine";

  private WebViewLocalServer localServer;
  private String CDV_LOCAL_SERVER;

  /** Used when created via reflection. */
  public IonicWebViewEngine(Context context, CordovaPreferences preferences) {
    super(new SystemWebView(context), preferences);
    Log.d(TAG, "Ionic Web View Engine Starting Right Up 1...");
  }

  public IonicWebViewEngine(SystemWebView webView) {
    super(webView, null);
    Log.d(TAG, "Ionic Web View Engine Starting Right Up 2...");
  }

  public IonicWebViewEngine(SystemWebView webView, CordovaPreferences preferences) {
    super(webView, preferences);
    Log.d(TAG, "Ionic Web View Engine Starting Right Up 3...");
  }

  //Is it useful? https://github.com/ionic-team/cordova-plugin-ionic-webview/issues/143
  // @Override
  // public void loadUrl(String url, boolean clearNavigationStack) {
  //   if (!url.startsWith("file:///android_asset/")) {
  //       url = url.replace("file:", CDV_LOCAL_SERVER + "/_file_");
  //   }
  //   super.loadUrl(url, clearNavigationStack);
  // }

  @Override
  public void init(CordovaWebView parentWebView, CordovaInterface cordova, final CordovaWebViewEngine.Client client,
                   CordovaResourceApi resourceApi, PluginManager pluginManager,
                   NativeToJsMessageQueue nativeToJsMessageQueue) {
    ConfigXmlParser parser = new ConfigXmlParser();
    parser.parse(cordova.getActivity());

    String port = preferences.getString("WKPort", "8080");
    CDV_LOCAL_SERVER = "http://localhost:" + port;

    localServer = new WebViewLocalServer(cordova.getActivity(), "localhost:" + port, true, parser);
    WebViewLocalServer.AssetHostingDetails ahd = localServer.hostAssets("www");

    webView.setWebViewClient(new ServerClient(this, parser));

    super.init(parentWebView, cordova, client, resourceApi, pluginManager, nativeToJsMessageQueue);
    SharedPreferences prefs = cordova.getActivity().getSharedPreferences(IonicWebView.WEBVIEW_PREFS_NAME, Activity.MODE_PRIVATE);
    String path = prefs.getString(IonicWebView.CDV_SERVER_PATH, null);
    if (path != null) {
      setServerBasePath(path);
    }
  }

  private class ServerClient extends SystemWebViewClient
  {
    private ConfigXmlParser parser;

    public ServerClient(SystemWebViewEngine parentEngine, ConfigXmlParser parser) {
      super(parentEngine);
      this.parser = parser;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      return localServer.shouldInterceptRequest(request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      if (url.equals(parser.getLaunchUrl())) {
        view.stopLoading();
        view.loadUrl(CDV_LOCAL_SERVER);
      }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      view.loadUrl("javascript:(function() { " +
              "window.WEBVIEW_SERVER_URL = '" + CDV_LOCAL_SERVER + "'" +
              "})()");
    }
  }

  public void setServerBasePath(String path){
    localServer.hostFiles(path);
    webView.loadUrl(CDV_LOCAL_SERVER);
  }

  public String getServerBasePath() {
    return this.localServer.getBasePath();
  }
}

