package br.com.giga.talhas

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context

class MainActivity : Activity() {
 private lateinit var web: WebView
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  web = WebView(this); web.settings.javaScriptEnabled = true; web.settings.domStorageEnabled = true
  web.webViewClient = WebViewClient(); web.addJavascriptInterface(Bridge(), "GigaAndroid")
  setContentView(web); web.loadUrl("file:///android_asset/index.html")
 }
 inner class Bridge {
  @JavascriptInterface fun printOs() { runOnUiThread {
   val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
   manager.print("GIGA TALHAS - OS", web.createPrintDocumentAdapter("OS Giga Talhas"), PrintAttributes.Builder().build())
  } }
 }
}
