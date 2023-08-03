package xyz.brilliant.argpt.ui.fragment

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.text.set
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import xyz.brilliant.argpt.R
import xyz.brilliant.argpt.ui.activity.BaseActivity
import xyz.brilliant.argpt.ui.adapter.ChatAdapter
import xyz.brilliant.argpt.ui.model.ChatModel
import java.io.IOException

class ChatGptFragment : Fragment() {
    private val client = OkHttpClient()
    // creating variables on below line.
//    lateinit var txtResponse: TextView
//    lateinit var idTVQuestion: TextView
    lateinit var etMessage: EditText
    lateinit var chatSend: ImageView

    lateinit var voiceSend : ImageView
    lateinit var settingBtn: ImageView
    lateinit var mainView: RelativeLayout
    lateinit var chatView: RecyclerView
    lateinit var chatAdapter: ChatAdapter
    lateinit var layoutManager: LinearLayoutManager
    //var chatMessages: List<ChatModel> = ArrayList()
    private val chatMessages = ArrayList<ChatModel>()
    lateinit var mView: View
    private lateinit var parentActivity: BaseActivity
    lateinit var connectionStatus : TextView
    fun updateConnectionStatus(status: String) {
        activity?.runOnUiThread {
            connectionStatus.text = status
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as BaseActivity
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mView= inflater.inflate(R.layout.activity_chat_gpt, container, false)

        etMessage=mView.findViewById(R.id.etMessage)
        chatSend=mView.findViewById(R.id.chatSend)
//        idTVQuestion=mView.findViewById(R.id.idTVQuestion)
//        txtResponse=mView.findViewById(R.id.txtResponse)
        settingBtn=mView.findViewById(R.id.settingBtn)
        mainView=mView.findViewById(R.id.mainView)
        chatView=mView.findViewById(R.id.chatView)
        voiceSend=mView.findViewById(R.id.voiceSend)
        layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true;
        //layoutManager.reverseLayout = true;
        chatView.layoutManager = layoutManager
        connectionStatus=mView.findViewById<TextView>(R.id.connectionStatus)
        chatAdapter = ChatAdapter(chatMessages)
        chatView.adapter = chatAdapter
        if(parentActivity.apiKey.isNullOrEmpty()){
            openChangeApiKey()
        }
        if(parentActivity.connectionStatus.isNotEmpty()){
            connectionStatus.text = parentActivity.connectionStatus
        }
        etMessage.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {

                // setting response tv on below line.
//                txtResponse.text = "Please wait.."

                // validating text
                val question = etMessage.text.toString().trim()
                Toast.makeText(activity,question, Toast.LENGTH_SHORT).show()
                if(question.isNotEmpty()){

                    getResponse(question) { response ->
                        activity?.runOnUiThread {
//                            txtResponse.text = response
                        }
                    }
                }
                return@OnEditorActionListener true
            }
            false
        })

        voiceSend.setOnClickListener {
            parentActivity.writeInt()
        }

        chatSend.setOnClickListener {
            if(etMessage.text.trim().isNotEmpty()){
                val question = etMessage.text.toString().trim()
                if(question.isNotEmpty()){
                    val singleChat = ChatModel(1,"S",question)
                    chatMessages.add(singleChat)
                    chatAdapter.notifyDataSetChanged()
                    parentActivity.getResponse(question)
                    etMessage.text.clear()
                }
            }
        }

        settingBtn.setOnClickListener {
            //showAtAnchor(mainView)
            showPopup()
        }
     //   parentActivity.sendHelloRaw("")
        return mView
    }
    fun updatechatList( type : String , msg : String){
        activity?.runOnUiThread {
            val singleChat = ChatModel(1, type, msg)
            chatMessages.add(singleChat)
            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }
    private lateinit var popupWindow: PopupWindow

    private fun showPopup() {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_layout, null)



        // Set up the popup window
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val ll_changeApiKey =popupView.findViewById<LinearLayout>(R.id.ll_changeApiKey)
        ll_changeApiKey.setOnClickListener {
            openChangeApiKey()
            popupWindow.dismiss()
        }
        val unpairMonocle =popupView.findViewById<LinearLayout>(R.id.unpair_monocle)
        unpairMonocle.setOnClickListener {
            parentActivity.unpairMonocle();
        }
        // Set up any additional settings for the popup window
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Show the popup below the icon
        popupWindow.showAtLocation(settingBtn,  Gravity.NO_GRAVITY, 220, 280)

        //popupWindow.showAsDropDown(settingBtn)
    }
    lateinit var dialog: Dialog;
    public fun openChangeApiKey() {
        dialog = Dialog(requireActivity(),R.style.TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.api_key_change_popup)
        val doneButton = dialog.findViewById<LinearLayout>(R.id.doneButton)
        val apiKeyText = dialog.findViewById<EditText>(R.id.apiKeyText)
        val closeButton = dialog.findViewById<LinearLayout>(R.id.closeButton)
        val apiKeyOld =  parentActivity.getStoredApiKey()
        apiKeyText.setText(apiKeyOld)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }


        doneButton.setOnClickListener {
            val apiKeyValue = apiKeyText.text.toString().trim()
            if (apiKeyValue.isNotEmpty()){
                // API key not null
                dialog.dismiss()
                parentActivity.storeApiKey(apiKeyValue)
                parentActivity.apiKey  = apiKeyValue
            }else{

                Toast.makeText(requireActivity(),"Please enter your OpenAI key",Toast.LENGTH_SHORT).show()

            }
        }
        dialog.show()
    }
    fun scrollToBottom() {
        chatView.scrollToPosition(chatMessages.size-1)
    }

    fun getResponse(question: String, callback: (String) -> Unit){
        try {


            // setting text on for question on below line.
//            idTVQuestion.text = question
            etMessage.setText("")

            //  val apiKey="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"
            //  val apiKey="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"
      //      val apiKey="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"
            // val apiKey="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"
            val url="https://api.openai.com/v1/engines/text-davinci-003/completions"

            val requestBody="""
            {
            "prompt": "$question",
            "max_tokens": 500,
            "temperature": 0
            }
        """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${parentActivity.apiKey}")
                .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("error","API failed",e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body=response.body?.string()
                    if (body != null) {
                        Log.v("data",body)
                    }
                    else{
                        Log.v("data","empty")
                    }
                    val jsonObject= JSONObject(body)
                    Log.d("TAG", "onResponse: "+jsonObject)

                    if (jsonObject.has("id")) {
                        val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                        val textResult = jsonArray.getJSONObject(0).getString("text")

                        parentActivity.sendChatGptResponce(textResult,"res:")
                        callback(textResult)
                    }else{
                        val error: JSONObject = jsonObject.getJSONObject("error")
                        val msg:String=error.getString("message")

                        parentActivity.sendChatGptResponce(msg,"err:")
                        activity?.runOnUiThread(Runnable {
                            //  Toast.makeText(this@ChatGptActivity,msg, Toast.LENGTH_SHORT).show()
//                            txtResponse.text = msg
                            val singleChat = ChatModel(1,"R",msg)
                            chatMessages.add(singleChat)
                            scrollToBottom()
                            chatAdapter.notifyDataSetChanged()

                        })
                    }
                }
            })
        }catch (ex:Exception){
            parentActivity.sendChatGptResponce("getResponse: $ex","err:")
            Log.d("ChatGpt", "getResponse: $ex")
        }
    }


}