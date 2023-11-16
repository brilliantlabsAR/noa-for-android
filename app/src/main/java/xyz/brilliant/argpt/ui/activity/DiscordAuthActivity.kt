package xyz.brilliant.argpt.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import xyz.brilliant.argpt.R

class DiscordAuthActivity : AppCompatActivity() {
    private val clientId = "1068143429038637148"
    private val redirectUri = "https://api.brilliant.xyz/noa/discord/callback"
    private val discordAuthUrl = "https://discord.com/oauth2/authorize" +
            "?response_type=code" +
            "&client_id=$clientId" +
            "&scope=identify" +
            "&redirect_uri=$redirectUri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discord_auth)

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith(redirectUri)) {
                    handleDiscordRedirect(url)
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.startsWith(redirectUri) == true) {
                    // Handle the redirect URL here
                    handleDiscordRedirect(url)
                }
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(discordAuthUrl)
    }
    private fun saveTokenAndEmailToSharedPreferences(token: String, email: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.putString("email", email)
        editor.apply()
    }
    private fun handleDiscordRedirect(redirectUrl: String) {
        val uri = Uri.parse(redirectUrl)
        val code = uri.getQueryParameter("code")

        if (!code.isNullOrBlank()) {
            // Exchange the authorization code for an access token
            runOnUiThread {
                Toast.makeText(this@DiscordAuthActivity, "code:$code", Toast.LENGTH_LONG)
                    .show()
            }

            lifecycleScope.launch(Dispatchers.Main) {
                // Add a delay of 5 seconds before starting the coroutine
                delay(5000)
                GlobalScope.launch(Dispatchers.IO) {
                    val response = makeSignInApiCall(code, "", "", "discord", "")
                    withContext(Dispatchers.Main) {
                        if (response != null) {
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()


                                var a = responseBody

                                val jsonResponse = JSONObject(responseBody)
                                val token = jsonResponse.optString("token")
                                val email = jsonResponse.optString("email")

                                saveTokenAndEmailToSharedPreferences(token, email)

                                runOnUiThread {
                                    Toast.makeText(
                                        this@DiscordAuthActivity,
                                        "Success $responseBody",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val intent =
                                        Intent(this@DiscordAuthActivity, BaseActivity::class.java)
                                    startActivity(intent)

                                    // Finish the current activity if you want to
                                    finish()
                                }
//
                                // Handle the response data as needed
                            } else {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@DiscordAuthActivity,
                                        "Failed" + response.body?.string(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                val responseBody = response.body?.string()
                                var a = responseBody
                                // Handle the error response
                                finish()
                            }
                        } else {

                            finish()
                            // Handle the exception or network error
                        }
                    }
                }

            }



        } else {
            finish()
            // Handle the case where the authorization code is not present
        }
    }

    fun makeSignInApiCall(idToken: String, name: String, email: String, socialType: String, socialId: String): Response? {
        val client = OkHttpClient()
        val mediaType = "text/plain".toMediaType()
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("id_token", idToken)
            .addFormDataPart("name", name)
            .addFormDataPart("email", email)
            .addFormDataPart("social_type", socialType)
            .addFormDataPart("social_id", socialId)
            .build()

        val request = Request.Builder()
            .url("https://api.brilliant.xyz/noa/signin")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }






    private fun handleTokenResponse(responseBody: String?) {
        // Parse and handle the JSON response containing the access token
        // You may want to store the access token securely or use it for API requests
    }
}
