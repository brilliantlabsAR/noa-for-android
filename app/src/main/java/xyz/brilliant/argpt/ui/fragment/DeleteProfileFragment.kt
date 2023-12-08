package xyz.brilliant.argpt.ui.fragment

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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DeleteProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DeleteProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var parentActivity: BaseActivity
    lateinit var btnDelete: Button
    lateinit var btngoBack: Button


    lateinit var mView: View
    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as BaseActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    fun onBackPressed() {
        // Get the fragment manager
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Check if there are fragments in the back stack
        if (fragmentManager.backStackEntryCount > 0) {
            // Pop the back stack to go to the previous fragment
            fragmentManager.popBackStack()
        } else {
            // If there are no fragments in the back stack, perform the default back press action
            requireActivity().onBackPressed()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_delete_profile, container, false)
        btnDelete = mView.findViewById(R.id.btnDelete)
        btngoBack= mView.findViewById(R.id.btngoBack)
        btnDelete.setOnClickListener {
            // Handle button click
           parentActivity.deleteAccount()
        }

        btngoBack.setOnClickListener {
            // Handle button click
            onBackPressed()
        }

        val privacyPolicyTextView: TextView = mView.findViewById(R.id.privacyPolicy)

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




        return mView;
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
    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DeleteProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}