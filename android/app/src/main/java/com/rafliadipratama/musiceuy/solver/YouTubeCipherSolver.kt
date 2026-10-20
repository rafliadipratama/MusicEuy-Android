package com.rafliadipratama.musiceuy.solver

import android.content.Context
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * YouTube JavaScript Cipher & N-Transform Solver engine (adopted from Musicfy).
 * Solves YouTube signature decipher and n-parameter transformation using
 * headless WebView executing yt.solver.core.js + meriyah + astring.
 */
object YouTubeCipherSolver {
    private const val TAG = "MusicEuy_Solver"
    private var solverWebView: SolverWebView? = null
    private val nParamCache = ConcurrentHashMap<String, String>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun transformNParamInUrl(context: Context, url: String): String {
        val nMatch = Regex("[?&]n=([^&]+)").find(url) ?: return url
        val nValue = Uri.decode(nMatch.groupValues[1])
        if (nValue.isBlank()) return url

        val cached = nParamCache[nValue]
        if (cached != null) {
            return url.replaceFirst(Regex("([?&])n=[^&]+"), "$1n=${Uri.encode(cached)}")
        }

        return withContext(NonCancellable) {
            try {
                val solver = getOrCreateSolver(context.applicationContext)
                if (solver == null || !solver.isAvailable) {
                    return@withContext url
                }

                val transformed = solver.transformN(nValue)
                if (transformed.isNotBlank() && transformed != nValue) {
                    nParamCache[nValue] = transformed
                    url.replaceFirst(Regex("([?&])n=[^&]+"), "$1n=${Uri.encode(transformed)}")
                } else {
                    url
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "n-transform failed: ${e.message}")
                url
            }
        }
    }

    private suspend fun getOrCreateSolver(context: Context): SolverWebView? {
        solverWebView?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                // Fetch player.js URL from YouTube
                val playerUrl = fetchPlayerJsUrl()
                val playerJs = if (!playerUrl.isNullOrBlank()) {
                    httpClient.newCall(Request.Builder().url(playerUrl).build()).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string() else null
                    }
                } else null

                withContext(Dispatchers.Main) {
                    if (playerJs.isNullOrBlank()) return@withContext null
                    SolverWebView.create(context, playerJs).also { solverWebView = it }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed creating solver: ${e.message}")
                null
            }
        }
    }

    private fun fetchPlayerJsUrl(): String? {
        try {
            val req = Request.Builder()
                .url("https://www.youtube.com/iframe_api")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val html = httpClient.newCall(req).execute().use { it.body?.string() } ?: return null
            val match = Regex("""player\\/([a-zA-Z0-9_-]+)\\/player_ias\.vflset\\/[a-zA-Z0-9_-]+\\/base\.js""").find(html)
                ?: Regex("""/s/player/[a-zA-Z0-9_-]+/player_ias\.vflset/[a-zA-Z0-9_-]+/base\.js""").find(html)
            if (match != null) {
                return "https://www.youtube.com" + match.value.replace("\\/", "/")
            }
        } catch (_: Exception) {}
        return null
    }

    class SolverWebView private constructor(
        context: Context,
        private val playerJs: String,
        private val initContinuation: Continuation<SolverWebView>
    ) {
        private val webView = WebView(context)
        private var nContinuation: Continuation<String>? = null
        var isAvailable: Boolean = false
            private set

        init {
            val settings = webView.settings
            @Suppress("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            settings.blockNetworkLoads = true

            webView.addJavascriptInterface(this, "EjsBridge")
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    return super.onConsoleMessage(m)
                }
            }
            loadSolverAndPlayer()
        }

        private fun loadSolverAndPlayer() {
            try {
                val cacheDir = File(webView.context.cacheDir, "ejs_solver").apply { mkdirs() }
                val assets = webView.context.assets

                for (file in listOf("meriyah.js", "astring.js", "yt.solver.core.js")) {
                    assets.open("solver/$file").use { input ->
                        File(cacheDir, file).outputStream().use { output -> input.copyTo(output) }
                    }
                }
                File(cacheDir, "player.js").writeText(playerJs)

                val html = """
                    <!DOCTYPE html>
                    <html><head>
                    <script src="meriyah.js"></script>
                    <script src="astring.js"></script>
                    <script src="yt.solver.core.js"></script>
                    <script>
                    var _nSolver = null;
                    function init() {
                        try {
                            var xhr = new XMLHttpRequest();
                            xhr.open('GET', 'player.js', true);
                            xhr.onload = function() {
                                try {
                                    var res = jsc({ type: 'player', player: xhr.responseText, requests: [], output_preprocessed: true });
                                    if (res && res.preprocessed_player) {
                                        var obj = {n: null, sig: null};
                                        (new Function('_result', res.preprocessed_player))(obj);
                                        _nSolver = obj.n;
                                        EjsBridge.onReady(_nSolver !== null);
                                    } else {
                                        EjsBridge.onError('Preprocess error');
                                    }
                                } catch(e) { EjsBridge.onError(e.message); }
                            };
                            xhr.send();
                        } catch(e) { EjsBridge.onError(e.message); }
                    }
                    function transformN(val) {
                        try {
                            if (_nSolver) {
                                EjsBridge.onNResult(_nSolver(val));
                            } else {
                                EjsBridge.onNResult(val);
                            }
                        } catch(e) { EjsBridge.onNResult(val); }
                    }
                    window.onload = init;
                    </script>
                    </head><body></body></html>
                """.trimIndent()

                webView.loadDataWithBaseURL("file://${cacheDir.absolutePath}/", html, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                initContinuation.resume(this)
            }
        }

        @JavascriptInterface
        fun onReady(hasN: Boolean) {
            isAvailable = hasN
            initContinuation.resume(this)
        }

        @JavascriptInterface
        fun onError(msg: String) {
            isAvailable = false
            initContinuation.resume(this)
        }

        @JavascriptInterface
        fun onNResult(result: String) {
            nContinuation?.resume(result)
            nContinuation = null
        }

        suspend fun transformN(nValue: String): String = suspendCancellableCoroutine { cont ->
            nContinuation = cont
            webView.post { webView.evaluateJavascript("transformN('${nValue.replace("'", "\\'")}');", null) }
        }

        companion object {
            suspend fun create(context: Context, playerJs: String): SolverWebView =
                suspendCancellableCoroutine { cont ->
                    SolverWebView(context, playerJs, cont)
                }
        }
    }
}
