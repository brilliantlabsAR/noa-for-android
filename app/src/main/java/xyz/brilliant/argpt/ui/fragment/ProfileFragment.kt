package xyz.brilliant.argpt.ui.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity
import xyz.brilliant.argpt.utils.Constant.PRIVACY_POLICY_URL


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    private lateinit var parentActivity: BaseActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as BaseActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_profile, container, false)
        val closeBtn=view.findViewById<ImageView>(R.id.closeBtn)
        val btnLogout=view.findViewById<TextView>(R.id.btnLogout)
        val btnDelAccount=view.findViewById<TextView>(R.id.btnDelAccount)
        val btnPrivacyPolicy=view.findViewById<TextView>(R.id.btnPrivacyPolicy)

        closeBtn.setOnClickListener {
            parentActivity.closeFragment()
        }

        btnLogout.setOnClickListener {
            parentActivity.unpairMonocle()
        }
        btnDelAccount.setOnClickListener {
            parentActivity.deleteAccount()
        }
        btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
            startActivity(intent)
        }

        return view
    }




}