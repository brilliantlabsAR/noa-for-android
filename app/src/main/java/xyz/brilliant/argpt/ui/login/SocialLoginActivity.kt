package xyz.brilliant.argpt.ui.login

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import xyz.brilliant.argpt.MainActivity
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity
import xyz.brilliant.argpt.ui.activity.DiscordAuthActivity


class SocialLoginActivity : AppCompatActivity() , GoogleApiClient.OnConnectionFailedListener {
    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val token = getTokenFromSharedPreferences()
        if (token.isNotEmpty()) {
            // If the token is not empty, navigate to BaseActivity and finish this activity
            val intent = Intent(this, BaseActivity::class.java)
            startActivity(intent)
            finish()
            return // Exit the function
        }
        setContentView(R.layout.activity_login_new)

        val btnGmail = findViewById<AppCompatButton>(R.id.btnGmail)
       // btnGmail.setSize(SignInButton.SIZE_STANDARD);

       // val btnApple = findViewById<Button>(R.id.btnApple)
        val btnDiscord = findViewById<AppCompatButton>(R.id.btnDiscord)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestIdToken("510809410914-fu0879edscc66bccj07as6g71vbutir2.apps.googleusercontent.com")
            .build()

        val privacyPolicyTextView: TextView = findViewById(R.id.privacyPolicy)

        val myString =
            SpannableString(getString(R.string.privecy_txt))

        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                //var d = "click1";
                gotoTerms("privacy");
            }
        }

        val clickableSpan1: ClickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                gotoTerms("terms");
            }
        }

        myString.setSpan(clickableSpan, 0, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        myString.setSpan(clickableSpan1, 19, 39, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        myString.setSpan(
            ForegroundColorSpan(Color.parseColor("#E82E87")),
            0,
            14,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        myString.setSpan(
            ForegroundColorSpan(Color.parseColor("#E82E87")),
            19,
            39,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        privacyPolicyTextView.movementMethod = LinkMovementMethod.getInstance()
        privacyPolicyTextView.text = myString


        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

        btnGmail.setOnClickListener {

            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
            startActivityForResult(signInIntent, RC_SIGN_IN)
            // Handle Gmail sign-in
            // Start the authentication process for Gmail
        }


        btnDiscord.setOnClickListener {

            val intent = Intent(this, DiscordAuthActivity::class.java)
            startActivity(intent)
            // Handle Discord sign-in
            // Start the authentication process for Discord
        }
    }

    private fun gotoTerms(url: String) {

        if(url=="terms") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://brilliant.xyz/pages/terms-conditions"))
            startActivity(intent)
        }
        else
        {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://brilliant.xyz/pages/privacy-policy"))
            startActivity(intent)
        }

    }
    private fun getTokenFromSharedPreferences(): String {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("token", "") ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = data?.let { Auth.GoogleSignInApi.getSignInResultFromIntent(it) }
            if (result != null) {
                if (result.isSuccess) {
                  //  Toast.makeText(this, "Sign-in was successful!", Toast.LENGTH_LONG).show()
                    // Google Sign-In was successful, handle the user's information
                    val account = result.signInAccount
                    val idToken = account?.idToken
                    val email = account?.email


                    GlobalScope.launch(Dispatchers.IO) {
                    val response = account?.displayName?.let {
                        makeSignInApiCall(idToken.toString(),
                            it, email.toString(), "1", account?.id.toString())
                    }
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
//                                        Toast.makeText(this@SocialLoginActivity, "Success $responseBody", Toast.LENGTH_LONG).show()

                                        val intent = Intent(this@SocialLoginActivity, BaseActivity::class.java)
                                        startActivity(intent)

                                        // Finish the current activity if you want to
                                        finish()
                                    }
//
                                    // Handle the response data as needed
                                } else {
                                    // Handle the error response
                                }
                            } else {
                                // Handle the exception or network error
                            }
                        }}



                    // You can use this information for user authentication or other purposes.
                } else {
                 //   Toast.makeText(this, "Sign-in was failed!", Toast.LENGTH_LONG).show()

                    // Google Sign-In failed
                }
            }
        }
    }
    private fun saveTokenAndEmailToSharedPreferences(token: String, email: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)
        editor.putString("email", email)
        editor.apply()
    }
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
     //   Toast.makeText(this, "onconnectionfailed", Toast.LENGTH_LONG).show()

        // Connection to Google Play services failed, handle this gracefully
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
}
