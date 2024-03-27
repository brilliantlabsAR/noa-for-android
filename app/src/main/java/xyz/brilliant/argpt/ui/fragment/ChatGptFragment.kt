package xyz.brilliant.argpt.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
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


class ChatGptFragment : Fragment(), ChatAdapter.OnItemClickListener {
    private val client = OkHttpClient()
    private lateinit var etMessage: EditText
    private lateinit var chatSend: ImageView
    private lateinit var popupWindow: PopupWindow
    private lateinit var profileBtn: ImageView
    private lateinit var mainView: RelativeLayout
    private lateinit var chatView: RecyclerView
    lateinit var chatAdapter: ChatAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var mView: View
    private lateinit var parentActivity: BaseActivity
    private lateinit var connectionStatus : ImageView
    private lateinit var btnTune : TextView

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        //mView= inflater.inflate(R.layout.activity_chat_gpt, container, false)
        mView= inflater.inflate(R.layout.activity_noa, container, false)
        etMessage=mView.findViewById(R.id.etMessage)
        chatSend=mView.findViewById(R.id.chatSend)
        profileBtn=mView.findViewById(R.id.profileBtn)
        mainView=mView.findViewById(R.id.mainView)
        chatView=mView.findViewById(R.id.chatView)
        layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        chatView.layoutManager = layoutManager
        connectionStatus=mView.findViewById(R.id.connectionStatus)
        btnTune=mView.findViewById(R.id.btnTune)
        chatAdapter = ChatAdapter(parentActivity.chatMessages,this)
        chatView.adapter = chatAdapter
        if(parentActivity.connectionStatus.isNotEmpty()){
            connectionStatus.visibility = View.VISIBLE
        }
        /**
         * Editor for message action listener
         */
        etMessage.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val question = etMessage.text.toString().trim()
                if(question.isNotEmpty()){
                    getResponse(question) {
                        activity?.runOnUiThread {

                        }
                    }
                }
                return@OnEditorActionListener true
            }
            false
        })



        /**
         * On Click event for send chat messages
         */
        chatSend.setOnClickListener {
            if(etMessage.text.trim().isNotEmpty()){
                val question = etMessage.text.toString().trim()
                if(question.isNotEmpty()){
                    val singleChat = ChatModel(1,"S",question)
                    parentActivity.chatMessages.add(singleChat)
                    chatAdapter.notifyDataSetChanged()
                    etMessage.text.clear()
                }
            }
        }
        /**
         * On Click event for open setting popup
         */
//        settingBtn.setOnClickListener {
//            showPopup(settingBtn)
//        }

        profileBtn.setOnClickListener {
            parentActivity.gotoProfileScreen()
        }


        /**
         * On Click event for go to tune screen
         */
        btnTune.setOnClickListener {
            parentActivity.gotoTuneScreen()
        }
        val tuneBox =mView.findViewById<LinearLayout>(R.id.tuneBox)
        val hackScreen =mView.findViewById<LinearLayout>(R.id.hackBox)

        tuneBox.setOnClickListener {
            parentActivity.gotoTuneScreen()
        }
        hackScreen.setOnClickListener {
            parentActivity.gotoHackScreen()
        }
        return mView
    }



    /**
     * Method to update connection status text
     */
    fun updateConnectionStatus(status: String) {
        activity?.runOnUiThread {
            if(status.isNotEmpty())
                connectionStatus.visibility = View.VISIBLE
            else
                connectionStatus.visibility = View.GONE
        }
    }
    /**
     * Method to attach activity context
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as BaseActivity
    }
    /**
     * Method to update chat list with text
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updatechatList(type : String, msg : String){
        activity?.runOnUiThread {
            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(1, type, msg.trim(),true)
                parentActivity.chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(1, type, msg.trim(),false)
                parentActivity.chatMessages.add(singleChat)
            }
            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Method to update chat list with image
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updatechatList(id : Int, type : String, msg : String, image :String){
        activity?.runOnUiThread {

            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(id, type, msg.trim(),true,image)
                parentActivity.chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(id, type, msg.trim(),false,image)
                parentActivity.chatMessages.add(singleChat)
            }


            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Method to update chat list with image
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updatechatList(id : Int, type : String, msg : String, image :Bitmap?){
        activity?.runOnUiThread {

            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(id, type, msg.trim(),true,"",image)
                parentActivity.chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(id, type, msg.trim(),false,"",image)
                parentActivity.chatMessages.add(singleChat)
            }


            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Method to show popup
     */
    @SuppressLint("InflateParams")
    private fun showPopup(anchorView: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_layout, null)



        // Set up the popup window
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val unpairMonocle =popupView.findViewById<LinearLayout>(R.id.unpair_monocle)


        val switchButton =popupView.findViewById<SwitchCompat>(R.id.switchButton)

        switchButton.isChecked = parentActivity.translateEnabled
        /**
         * On Click event for switch language
         */
        switchButton.setOnClickListener {

            parentActivity.translateEnabled =switchButton.isChecked

        }

        val deleteProfileLayout = popupView.findViewById<LinearLayout>(R.id.delete_profile_layout)

        deleteProfileLayout.visibility = View.VISIBLE


        /**
         * On Click event for delete profile
         */
        deleteProfileLayout.setOnClickListener{
            popupWindow.dismiss()
            parentActivity.gotoDeleteProfile()
        }


        /**
         * On Click event for unpair device
         */
        unpairMonocle.setOnClickListener {
            popupWindow.dismiss()
            parentActivity.unpairMonocle()
        }
        // Set up any additional settings for the popup window
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true


        // Calculate the xOffset to end on the left of anchorView
        val xOffset = -30

        // Calculate the yOffset to be below anchorView
        val yOffset = 30  // Adjust the top margin here
        popupWindow.showAsDropDown(anchorView, xOffset, yOffset)

    }

    /**
     * Method to scroll to bottom
     */
    fun scrollToBottom() {
        chatView.scrollToPosition(parentActivity.chatMessages.size-1)
    }
    /**
     * REST API response listener for chat message
     */
    private fun getResponse(question: String, callback: (String) -> Unit){
        try {

            etMessage.setText("")


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

                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(call: Call, response: Response) {
                    val body=response.body?.string()
                    if (body != null) {
                        Log.v("data",body)
                    }
                    else{
                        Log.v("data","empty")
                    }
                    val jsonObject= JSONObject(body!!)
                    if (jsonObject.has("id")) {
                        val jsonArray: JSONArray = jsonObject.getJSONArray("choices")
                        val textResult = jsonArray.getJSONObject(0).getString("text")

                        parentActivity.sendChatGptResponce(textResult,"res:")
                        callback(textResult)
                    }else{
                        val error: JSONObject = jsonObject.getJSONObject("error")
                        val msg:String=error.getString("message")

                        parentActivity.sendChatGptResponce(msg,"err:")
                        activity?.runOnUiThread {
                            val singleChat = ChatModel(1, "R", msg)
                            parentActivity.chatMessages.add(singleChat)
                            scrollToBottom()
                            chatAdapter.notifyDataSetChanged()

                        }
                    }
                }
            })
        }catch (ex:Exception){
            parentActivity.sendChatGptResponce("getResponse: $ex","err:")
            Log.d("ChatGpt", "getResponse: $ex")
        }
    }

    /**
     * Not being used
     */
    override fun onUrlClick(position: Int, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    /**
     * Method to show full screen image
     */
    override fun onImageClick(position: Int, url: String, bitmap: Bitmap?) {
        val fullScreenPopup = FullScreenPopup(parentActivity, url,bitmap)
        fullScreenPopup.show()
    }

    /**
     * Not being used
     */
    override fun onStabilityApiClick(position: Int, chatModel: ChatModel) {
        // gotoStabilityApi()
        // stabilityChangeApiKey()
    }

    /**
     * Not being used
     */
    override fun onOpenApiClick(position: Int, chatModel: ChatModel) {
        //  openChangeApiKey()
    }


}