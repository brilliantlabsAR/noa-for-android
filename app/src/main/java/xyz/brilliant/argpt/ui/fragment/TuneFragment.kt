package xyz.brilliant.argpt.ui.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity


/**
 * A simple [Fragment] subclass.
 * Use the [TuneFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TuneFragment : Fragment() {

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
        val view= inflater.inflate(R.layout.fragment_tune, container, false)
        val chatBox=view.findViewById<LinearLayout>(R.id.chatBox)
        val hackBox=view.findViewById<LinearLayout>(R.id.hackBox)
        val profileBtn=view.findViewById<ImageView>(R.id.profileBtn)


        chatBox.setOnClickListener {
            parentActivity.gotoNext()
        }
        hackBox.setOnClickListener {
            parentActivity.gotoHackScreen()
        }
        profileBtn.setOnClickListener {
            parentActivity.gotoProfileScreen()
        }
        return view
    }


}