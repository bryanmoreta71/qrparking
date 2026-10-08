package com.qrparking.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var webView: WebView
    private lateinit var progress: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_home -> { loadHome(); true }
                R.id.action_fill_login -> { fillLogin(manual = true); true }
                R.id.action_reload -> { webView.reload(); true }
                R.id.action_settings -> { showSettings(); true }
                else -> false
            }
        }

        progress = findViewById(R.id.progress)
        webView = findViewById(R.id.webView)
        setupWebView()

        findViewById<ExtendedFloatingActionButton>(R.id.fabScan).setOnClickListener { scan() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else if (prefs.url.isBlank()) {
            showSettings()
        } else {
            loadHome()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Las cookies se guardan, así la sesión del centro comercial se mantiene entre usos.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                view.evaluateJavascript(FillScript.TRACK_FOCUS, null)
                if (prefs.autoLogin) fillLogin(manual = false)
                CookieManager.getInstance().flush()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.setProgressCompat(newProgress, true)
            }
        }
    }

    private fun loadHome() {
        val url = prefs.url
        if (url.isBlank()) {
            Toast.makeText(this, R.string.url_required, Toast.LENGTH_SHORT).show()
            showSettings()
        } else {
            webView.loadUrl(url)
        }
    }

    private fun scan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(this, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: barcode.displayValue ?: return@addOnSuccessListener
                fillCode(raw)
            }
            .addOnFailureListener { e ->
                // El usuario cerró el escáner: no es un error que haya que mostrar.
                if (e is MlKitException && e.errorCode == MlKitException.CODE_SCANNER_CANCELLED) return@addOnFailureListener
                Snackbar.make(webView, getString(R.string.scan_failed, e.localizedMessage ?: ""), Snackbar.LENGTH_LONG).show()
            }
    }

    private fun fillCode(raw: String) {
        val code = if (prefs.digitsOnly) raw.filter { it.isDigit() } else raw.trim()
        webView.evaluateJavascript(FillScript.fill(code, prefs.selector.trim(), prefs.autoSubmit)) { result ->
            val msg = if (result?.contains("OK") == true) {
                getString(R.string.code_filled, code)
            } else {
                getString(R.string.field_not_found, code)
            }
            Snackbar.make(webView, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    /** Solo escribimos la contraseña en el sitio configurado (mismo dominio o subdominio). */
    private fun isTrustedPage(): Boolean {
        val pageHost = webView.url?.let { Uri.parse(it).host } ?: return false
        val baseHost = Uri.parse(prefs.url).host?.removePrefix("www.") ?: return false
        return pageHost == baseHost || pageHost.endsWith(".$baseHost")
    }

    private fun fillLogin(manual: Boolean) {
        val user = prefs.username
        val pass = prefs.password
        if (user.isEmpty() && pass.isEmpty()) {
            if (manual) Snackbar.make(webView, R.string.no_credentials, Snackbar.LENGTH_LONG).show()
            return
        }
        if (!isTrustedPage()) return
        webView.evaluateJavascript(FillScript.login(user, pass), null)
    }

    private fun showSettings() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val editUrl = view.findViewById<EditText>(R.id.editUrl)
        val editSelector = view.findViewById<EditText>(R.id.editSelector)
        val switchDigits = view.findViewById<MaterialSwitch>(R.id.switchDigits)
        val switchSubmit = view.findViewById<MaterialSwitch>(R.id.switchSubmit)
        val editUser = view.findViewById<EditText>(R.id.editUser)
        val editPass = view.findViewById<EditText>(R.id.editPass)
        val switchAutoLogin = view.findViewById<MaterialSwitch>(R.id.switchAutoLogin)

        editUrl.setText(prefs.url)
        editSelector.setText(prefs.selector)
        switchDigits.isChecked = prefs.digitsOnly
        switchSubmit.isChecked = prefs.autoSubmit
        editUser.setText(prefs.username)
        editPass.setText(prefs.password)
        switchAutoLogin.isChecked = prefs.autoLogin

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                var url = editUrl.text.toString().trim()
                if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                if (url.isEmpty() || android.util.Patterns.WEB_URL.matcher(url).matches().not()) {
                    editUrl.error = getString(R.string.invalid_url)
                    return@setOnClickListener
                }
                val urlChanged = url != prefs.url
                prefs.url = url
                prefs.selector = editSelector.text.toString().trim()
                prefs.digitsOnly = switchDigits.isChecked
                prefs.autoSubmit = switchSubmit.isChecked
                prefs.username = editUser.text.toString().trim()
                prefs.password = editPass.text.toString()
                prefs.autoLogin = switchAutoLogin.isChecked
                dialog.dismiss()
                if (urlChanged || webView.url == null) loadHome()
                else if (prefs.autoLogin) fillLogin(manual = false)
            }
        }
        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}
