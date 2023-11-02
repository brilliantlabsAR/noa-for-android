package xyz.brilliant.argpt.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import xyz.brilliant.argpt.MainActivity
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity


class SocialLoginActivity : AppCompatActivity() , GoogleApiClient.OnConnectionFailedListener {
    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleApiClient: GoogleApiClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_login)

        val btnGmail = findViewById<Button>(R.id.btnGmail)
        val btnApple = findViewById<Button>(R.id.btnApple)
        val btnDiscord = findViewById<Button>(R.id.btnDiscord)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        val mGoogleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(this,gso)

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()

        btnGmail.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
//            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
//            startActivityForResult(signInIntent, RC_SIGN_IN)
            // Handle Gmail sign-in
            // Start the authentication process for Gmail
        }

        btnApple.setOnClickListener {
            // Handle Apple sign-in
            // Start the authentication process for Apple
        }

        btnDiscord.setOnClickListener {
            // Handle Discord sign-in
            // Start the authentication process for Discord
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
             val id =   account.id
                // You have successfully signed in. You can access account information here.
            } catch (e: ApiException) {
                // Handle sign-in failure (e.getStatusMessage() will contain a description of the error).
            }
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // Connection to Google Play services failed, handle this gracefully
    }
}
