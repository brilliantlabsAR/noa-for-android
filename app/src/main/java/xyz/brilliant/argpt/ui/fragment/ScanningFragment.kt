package xyz.brilliant.argpt.ui.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity


class ScanningFragment : Fragment() {

    private lateinit var parentActivity: BaseActivity
    private lateinit var deviceCloseTextView: TextView
    private lateinit var popUpbtn: TextView
    private lateinit var myCardView: CardView
    private lateinit var searchBox: RelativeLayout

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as BaseActivity
    }


    fun updatePopUp(deviceCloseText: String,buttonText:String){
        parentActivity.runOnUiThread {
            deviceCloseTextView.text = deviceCloseText
            popUpbtn.text = buttonText
            popUpbtn.isClickable=false
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myCardView = view.findViewById(R.id.myCardView)
        popUpbtn = view.findViewById(R.id.popUpbtn)
        deviceCloseTextView = view.findViewById(R.id.deviceCloseTextView)
        searchBox = view.findViewById(R.id.searchBox)







        myCardView.translationY = myCardView.height.toFloat()

        val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                myCardView.viewTreeObserver.removeOnPreDrawListener(this)
                myCardView.animate().translationY(-200f).setDuration(1000).start()
                return true
            }
        }

        myCardView.viewTreeObserver.addOnPreDrawListener(preDrawListener)

        searchBox.setOnClickListener {
            try {
                if (popUpbtn.text == "Monocle. Connect") {
                    parentActivity.connectDevice()
                }
            }catch (ex:Exception){
                ex.printStackTrace()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_pairing_screen, container, false)

        val nextPageButton = view.findViewById<Button>(R.id.btnStartScan)

        val privacyPolicyTextView: TextView = view.findViewById(R.id.privacyPolicy)

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

        myString.setSpan(clickableSpan, 20, 34, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        myString.setSpan(clickableSpan1, 48, 68, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        myString.setSpan(
            ForegroundColorSpan(Color.parseColor("#E82E87")),
            20,
            34,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        myString.setSpan(
            ForegroundColorSpan(Color.parseColor("#E82E87")),
            48,
            68,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        privacyPolicyTextView.movementMethod = LinkMovementMethod.getInstance()
        privacyPolicyTextView.text = myString




        return view
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
}