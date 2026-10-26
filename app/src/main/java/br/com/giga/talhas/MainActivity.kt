package br.com.giga.talhas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import android.webkit.WebResourceRequest
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintManager
import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
 private lateinit var web: WebView
 private var ultimoVoltarEm: Long = 0L
 private var backupPending: String? = null
 private var fileCallback: ValueCallback<Array<Uri>>? = null
 private var cameraUri: Uri? = null
 private val PHOTO_REQUEST = 203
 private val EXPORT_REQUEST = 201
 private val IMPORT_REQUEST = 202
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  web = WebView(this)
  web.settings.javaScriptEnabled = true
  web.settings.domStorageEnabled = true
  web.webViewClient = WebViewClient()
  web.webChromeClient = object : WebChromeClient() {
   override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
    fileCallback?.onReceiveValue(null)
    fileCallback = callback
    return try {
     val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "image/*"
      addCategory(Intent.CATEGORY_OPENABLE)
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
     }
     val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
     val options = mutableListOf<Intent>()
     if (camera.resolveActivity(packageManager) != null) {
      val folder = File(cacheDir, "photos").apply { mkdirs() }
      val file = File.createTempFile("giga_", ".jpg", folder)
      cameraUri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", file)
      camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
      camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      options.add(camera)
     }
     val chooser = Intent.createChooser(pick, "Selecionar ou tirar foto")
     chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, options.toTypedArray())
     startActivityForResult(chooser, PHOTO_REQUEST)
     true
    } catch (e: Exception) {
     fileCallback?.onReceiveValue(null)
     fileCallback = null
     false
    }
   }
  }
  web.addJavascriptInterface(Bridge(), "GigaAndroid")
  setContentView(web)
  web.loadUrl("file:///android_asset/index.html")
 }
 override fun onBackPressed() {
  val agora = SystemClock.elapsedRealtime()
  if (agora - ultimoVoltarEm <= 2000L) {
   super.onBackPressed()
  } else {
   ultimoVoltarEm = agora
   Toast.makeText(this, "Pressione Voltar novamente para sair do GIGA TALHAS.", Toast.LENGTH_SHORT).show()
  }
 }
 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if (requestCode == PHOTO_REQUEST) {
   val uris = if (resultCode == RESULT_OK) {
    val clip = data?.clipData
    when {
     clip != null -> Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
     data?.data != null -> arrayOf(data.data!!)
     else -> cameraUri?.let { arrayOf(it) } ?: emptyArray()
    }
   } else emptyArray()
   fileCallback?.onReceiveValue(if (uris.isEmpty()) null else uris)
   fileCallback = null
   cameraUri = null
   return
  }
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
  @JavascriptInterface fun shareText(value: String) { runOnUiThread {
   val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "GIGA TALHAS - Ordens de serviço")
    putExtra(Intent.EXTRA_TEXT, value)
   }
   startActivity(Intent.createChooser(intent, "Enviar ordens de serviço"))
  } }

  @JavascriptInterface fun consultarCnpj(cnpj: String, campo: String) {
   if (!cnpj.matches(Regex("[0-9]{14}")) || campo !in listOf("cli","exe")) return
   Thread {
    var response = ""
    try {
     val connection = URL("https://brasilapi.com.br/api/cnpj/v1/$cnpj").openConnection() as HttpURLConnection
     connection.connectTimeout = 12000
     connection.readTimeout = 12000
     connection.setRequestProperty("Accept", "application/json")
     connection.setRequestProperty("User-Agent", "GigaTalhas/0.6")
     try {
      if (connection.responseCode == 200) {
       response = connection.inputStream.bufferedReader().use { it.readText() }
      } else {
       response = JSONObject().put("erro", "Consulta indisponível ou CNPJ não localizado (HTTP ${connection.responseCode})").toString()
      }
     } finally { connection.disconnect() }
    } catch (e: Exception) {
     response = JSONObject().put("erro", "Não foi possível consultar. Verifique a internet e tente novamente.").toString()
    }
    val callback = "receberCnpj(${JSONObject.quote(campo)},${JSONObject.quote(cnpj)},${JSONObject.quote(response)})"
    runOnUiThread { web.evaluateJavascript(callback, null) }
   }.start()
  }

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
  @JavascriptInterface fun sharePdf(html: String, fileName: String, destino: String) { runOnUiThread {
   WebView.enableSlowWholeDocumentDraw()
   val printable = WebView(this@MainActivity)
   printable.settings.javaScriptEnabled = false
   printable.settings.defaultTextEncodingName = "UTF-8"
   printable.settings.useWideViewPort = false
   printable.settings.loadWithOverviewMode = false
   printable.setInitialScale(100)
   printable.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
   val root = window.decorView as android.view.ViewGroup
   // Renderizar fora da area visivel, mas manter a view anexada para carregar imagens.
   root.addView(printable, 0, android.view.ViewGroup.LayoutParams(794, 1123))
   var started = false
   fun cleanup() {
    try { root.removeView(printable); printable.destroy() } catch (_: Exception) {}
   }
   fun fail(message: String) {
    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    web.evaluateJavascript("alert(" + JSONObject.quote(message) + ")", null)
    cleanup()
   }
   printable.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
     if (started) return
     started = true
     printable.postDelayed({
      try {
       // Uma pagina A4 corresponde a 794 x 1123 pixels CSS em 96 dpi.
       // Renderizar com largura fixa, independente da densidade fisica do celular.
       val cssWidth = 794
       val pageWidth = 595
       val pageHeight = 842
       val scale = pageWidth.toFloat() / cssWidth
       val sourcePageHeight = (pageHeight / scale).toInt()
       printable.measure(
        android.view.View.MeasureSpec.makeMeasureSpec(cssWidth, android.view.View.MeasureSpec.EXACTLY),
        android.view.View.MeasureSpec.makeMeasureSpec(sourcePageHeight, android.view.View.MeasureSpec.EXACTLY)
       )
       printable.layout(0, 0, cssWidth, sourcePageHeight)
       val cssHeight = printable.contentHeight.coerceAtLeast(sourcePageHeight)
       val pages = ((cssHeight + sourcePageHeight - 1) / sourcePageHeight).coerceIn(1, 100)
       val pdf = android.graphics.pdf.PdfDocument()
       for (i in 0 until pages) {
        printable.scrollTo(0, i * sourcePageHeight)
        val page = pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create())
        page.canvas.drawColor(android.graphics.Color.WHITE)
        page.canvas.save()
        page.canvas.scale(scale, scale)
        printable.draw(page.canvas)
        page.canvas.restore()
        pdf.finishPage(page)
       }
       val folder = File(cacheDir, "shared_os").apply { mkdirs() }
       val safeName = fileName.replace(Regex("[^a-zA-Z0-9_.-]"), "_").take(100)
       val output = File(folder, if (safeName.endsWith(".pdf")) safeName else "$safeName.pdf")
       output.outputStream().use { pdf.writeTo(it) }
       pdf.close()
       if (!output.exists() || output.length() == 0L) throw Exception("PDF vazio")
       val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", output)
       val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "GIGA TALHAS - Ordem de Serviço")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(contentResolver, "Ordem de Serviço PDF", uri)
       }
       try {
        if (destino == "whatsapp") startActivity(Intent(send).apply { setPackage("com.whatsapp") })
        else startActivity(Intent.createChooser(send, "Compartilhar PDF da O.S."))
       } catch (_: Exception) {
        startActivity(Intent.createChooser(send, "Escolher aplicativo para compartilhar PDF"))
       }
       cleanup()
      } catch (e: Exception) { fail("Falha ao gerar ou compartilhar PDF: " + (e.message ?: "erro desconhecido")) }
     }, 250)
    }
   }
   printable.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  } }

  @JavascriptInterface fun printHtml(html: String) { runOnUiThread {
   val printable = WebView(this@MainActivity)
   printable.settings.javaScriptEnabled = false
   printable.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
     val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
     manager.print("GIGA TALHAS - OS", printable.createPrintDocumentAdapter("OS Giga Talhas"), PrintAttributes.Builder().build())
    }
   }
   printable.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  } }
  @JavascriptInterface fun printOs() { runOnUiThread {
   val manager = getSystemService(Context.PRINT_SERVICE) as PrintManager
   manager.print("GIGA TALHAS - OS", web.createPrintDocumentAdapter("OS Giga Talhas"), PrintAttributes.Builder().build())
  } }
 }
}
