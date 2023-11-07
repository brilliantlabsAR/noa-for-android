package xyz.brilliant.argpt.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
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


class SocialLoginActivity : AppCompatActivity() , GoogleApiClient.OnConnectionFailedListener {
    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = getTokenFromSharedPreferences()
        if (token.isNotEmpty()) {
            // If the token is not empty, navigate to BaseActivity and finish this activity
            val intent = Intent(this, BaseActivity::class.java)
            startActivity(intent)
            finish()
            return // Exit the function
        }
        setContentView(R.layout.activity_social_login)

        val btnGmail = findViewById<Button>(R.id.btnGmail)
       // val btnApple = findViewById<Button>(R.id.btnApple)
        val btnDiscord = findViewById<Button>(R.id.btnDiscord)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestIdToken("510809410914-fu0879edscc66bccj07as6g71vbutir2.apps.googleusercontent.com")
            .build()



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
            // Handle Discord sign-in
            // Start the authentication process for Discord
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
                    Toast.makeText(this, "Sign-in was successful!", Toast.LENGTH_LONG).show()
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
                                        Toast.makeText(this@SocialLoginActivity, "Success $responseBody", Toast.LENGTH_LONG).show()

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
                    Toast.makeText(this, "Sign-in was failed!", Toast.LENGTH_LONG).show()

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
        Toast.makeText(this, "onconnectionfailed", Toast.LENGTH_LONG).show()

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
