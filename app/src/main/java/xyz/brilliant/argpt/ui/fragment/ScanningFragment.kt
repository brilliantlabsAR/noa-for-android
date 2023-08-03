package xyz.brilliant.argpt.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
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
                if (popUpbtn.text == "Connect") {
                    parentActivity.connectDevice()
                }else if (popUpbtn.text == "Continue"){
                    parentActivity.fileUploadOne()
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




//        view.findViewById<TextView>(R.id.searching).setOnClickListener {
//            // Navigate to PageTwoFragment
//            requireActivity().supportFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, PageTwoFragment())
//                .commit()
//        }

        return view
    }
}