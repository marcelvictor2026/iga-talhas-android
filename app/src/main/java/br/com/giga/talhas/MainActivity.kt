package br.com.giga.talhas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context
import org.json.JSONObject

class MainActivity : Activity() {
 private lateinit var web: WebView
 private var backupPending: String? = null
 private val EXPORT_REQUEST = 201
 private val IMPORT_REQUEST = 202
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  web = WebView(this)
  web.settings.javaScriptEnabled = true
  web.settings.domStorageEnabled = true
  web.webViewClient = WebViewClient()
  web.addJavascriptInterface(Bridge(), "GigaAndroid")
  setContentView(web)
  web.loadUrl("file:///android_asset/index.html")
 }
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if (resultCode != RESULT_OK || data?.data == null) return
  try {
   if (requestCode == EXPORT_REQUEST) {
    val value = backupPending ?: return
    contentResolver.openOutputStream(data.data!!)?.use { it.write(value.toByteArray(Charsets.UTF_8)) }
    backupPending = null
    web.evaluateJavascript("alert('Backup salvo com sucesso')", null)
   } else if (requestCode == IMPORT_REQUEST) {
    val value = contentResolver.openInputStream(data.data!!)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: return
    web.evaluateJavascript("restaurarBackupRecebido(${JSONObject.quote(value)})", null)
   }
  } catch (e: Exception) {
   web.evaluateJavascript("alert('Falha ao acessar o arquivo de backup')", null)
  }
 }
 inner class Bridge {
  @JavascriptInterface fun exportBackup(json: String) { runOnUiThread {
   backupPending = json
   val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/json"
    putExtra(Intent.EXTRA_TITLE, "GIGA-TALHAS-backup.json")
   }
   startActivityForResult(intent, EXPORT_REQUEST)
  } }
  @JavascriptInterface fun importBackup() { runOnUiThread {
   val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "application/json"
   }
   startActivityForResult(intent, IMPORT_REQUEST)
  } }
  @JavascriptInterface fun printOs() { runOnUiThread {
   val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
   manager.print("GIGA TALHAS - OS", web.createPrintDocumentAdapter("OS Giga Talhas"), PrintAttributes.Builder().build())
  } }
 }
}
