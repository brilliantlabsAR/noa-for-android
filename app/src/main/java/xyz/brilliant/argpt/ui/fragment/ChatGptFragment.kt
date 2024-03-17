package xyz.brilliant.argpt.ui.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.appcompat.app.AlertDialog
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
import android.widget.EditText

class ChatGptFragment : Fragment(), ChatAdapter.OnItemClickListener {
    companion object {
        private const val ARG_API_KEY = "api_key"
        private const val ARG_ENDPOINT = "endpoint"
        private const val ARG_MODEL = "model"
        private const val ARG_SYSTEM_MESSAGE = "system_message"

        fun newInstance(apiKey: String, endpoint: String, model: String, systemMessage: String): ChatGptFragment {
            val fragment = ChatGptFragment()
            val args = Bundle()
            args.putString(ARG_API_KEY, apiKey)
            args.putString(ARG_ENDPOINT, endpoint)
            args.putString(ARG_MODEL, model)
            args.putString(ARG_SYSTEM_MESSAGE, systemMessage)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var openaiApiKey: String
    private lateinit var openaiEndpoint: String
    private lateinit var openaiModel: String
    private lateinit var openaiSystemMessage: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            openaiApiKey = it.getString(ARG_API_KEY) ?: "none"
            openaiEndpoint = it.getString(ARG_ENDPOINT) ?: "https://api.openai.com/v1"
            openaiModel = it.getString(ARG_MODEL) ?: "gpt-4-vision-preview"
            openaiSystemMessage = it.getString(ARG_SYSTEM_MESSAGE) ?: "You are a helpful assistant."
        }
    }
    private val client = OkHttpClient()
    // creating variables on below line.
//    lateinit var txtResponse: TextView
//    lateinit var idTVQuestion: TextView
    lateinit var etMessage: EditText
    lateinit var chatSend: ImageView
    private lateinit var popupWindow: PopupWindow
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
    lateinit var connectionStatus : ImageView
    fun updateConnectionStatus(status: String) {
        activity?.runOnUiThread {
            if(status.isNotEmpty())
            connectionStatus.visibility = View.VISIBLE
            else
                connectionStatus.visibility = View.GONE
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
    ): View {
        // Inflate the layout for this fragment
        mView= inflater.inflate(R.layout.activity_chat_gpt, container, false)

        etMessage=mView.findViewById(R.id.etMessage)
        chatSend=mView.findViewById(R.id.chatSend)
//        idTVQuestion=mView.findViewById(R.id.idTVQuestion)
//        txtResponse=mView.findViewById(R.id.txtResponse)
        settingBtn=mView.findViewById(R.id.settingBtn)
        mainView=mView.findViewById(R.id.mainView)
        chatView=mView.findViewById(R.id.chatView)
        //voiceSend=mView.findViewById(R.id.voiceSend)
        layoutManager = LinearLayoutManager(activity)
        layoutManager.stackFromEnd = true
        //layoutManager.reverseLayout = true;
        chatView.layoutManager = layoutManager
        connectionStatus=mView.findViewById<ImageView>(R.id.connectionStatus)
        chatAdapter = ChatAdapter(chatMessages,this)
        chatView.adapter = chatAdapter
        if(parentActivity.apiKey.isNullOrEmpty()){
            openChangeApiKey()
        }
        if(parentActivity.connectionStatus.isNotEmpty()){
            connectionStatus.visibility = View.VISIBLE
           // connectionStatus.text = parentActivity.connectionStatus
        }
        etMessage.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {

                // setting response tv on below line.
//                txtResponse.text = "Please wait.."

                // validating text
                val question = etMessage.text.toString().trim()
              //  Toast.makeText(activity,question, Toast.LENGTH_SHORT).show()
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

     //   parentActivity.sendHelloRaw("")
        return mView
    }
    fun updatechatList( type : String , msg : String){
        activity?.runOnUiThread {

            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(1, type, msg.trim(),true)
                chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(1, type, msg.trim(),false)
                chatMessages.add(singleChat)
            }


            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    fun updatechatList(id : Int ,type : String,msg : String, image :String){
        activity?.runOnUiThread {

            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(id, type, msg.trim(),true,image)
                chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(id, type, msg.trim(),false,image)
                chatMessages.add(singleChat)
            }


            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    fun updatechatList(id : Int ,type : String,msg : String, image :Bitmap?){
        activity?.runOnUiThread {

            if(parentActivity.translateEnabled)
            {
                val singleChat = ChatModel(id, type, msg.trim(),true,"",image)
                chatMessages.add(singleChat)
            }
            else
            {
                val singleChat = ChatModel(id, type, msg.trim(),false,"",image)
                chatMessages.add(singleChat)
            }


            scrollToBottom()
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun showPopup() {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_layout, null)
    
        // Set up the popup window
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val ll_changeApiKey = popupView.findViewById<LinearLayout>(R.id.ll_changeApiKey)
        ll_changeApiKey.setOnClickListener {
            openChangeApiKey()
            popupWindow.dismiss()
        }
        val unpairMonocle = popupView.findViewById<LinearLayout>(R.id.unpair_monocle)
        
        val switchButton = popupView.findViewById<Switch>(R.id.switchButton)
    
        switchButton.isChecked = parentActivity.translateEnabled
    
        switchButton.setOnClickListener {
            parentActivity.translateEnabled = switchButton.isChecked
            popupWindow.dismiss()
        }
        
        unpairMonocle.setOnClickListener {
            popupWindow.dismiss()
            parentActivity.unpairMonocle()
        }
        // Set up any additional settings for the popup window
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
    
        // Show the popup below the icon
        val location = IntArray(2)
        settingBtn.getLocationOnScreen(location)
        val x = location[0] + settingBtn.width - popupWindow.width // Adjust the space here
        val y = location[1] - popupWindow.height
        popupWindow.showAtLocation(settingBtn, Gravity.NO_GRAVITY, x, y + 10)
    }
    private fun gotoOpenApi() {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com"))
            startActivity(intent)
    }

    private fun gotoStabilityApi() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://key.stabediffusion.com"))
        startActivity(intent)
    }

    lateinit var dialog: Dialog
    fun openChangeApiKey() {
        dialog = Dialog(requireActivity(),R.style.TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.api_key_change_popup_open_ai_stability_api)
        val doneButton = dialog.findViewById<LinearLayout>(R.id.doneButton)
        val apiKeyText = dialog.findViewById<EditText>(R.id.apiKeyText)
        val apiKeyTextStabilityApi = dialog.findViewById<EditText>(R.id.apiKeyTextStabilityApi)
        val openAiEndpoint = dialog.findViewById<EditText>(R.id.openAiEndpoint)
        val openAiModel = dialog.findViewById<EditText>(R.id.openAiModel)
        val systemMessage = dialog.findViewById<EditText>(R.id.systemMessage)
//        val closeButton = dialog.findViewById<LinearLayout>(R.id.closeButton)
        val apiKeyOld =  parentActivity.getStoredApiKey()
        apiKeyText.setText(apiKeyOld)

        val oldStabilityApiKey =  parentActivity.getStoredStabilityApiKey()
        apiKeyTextStabilityApi.setText(oldStabilityApiKey)
//        closeButton.setOnClickListener {
//            dialog.dismiss()
//            gotoOpenApi()
//        }


        doneButton.setOnClickListener {
            val apiKeyValue = apiKeyText.text.toString().trim()
            if (apiKeyValue.isNotEmpty()){
                // API key not null
                dialog.dismiss()
                parentActivity.storeApiKey(apiKeyValue)
                parentActivity.apiKey  = apiKeyValue
                parentActivity.storeStabilityApiKey(apiKeyTextStabilityApi.text.toString().trim())
                parentActivity.stabilityApiKey  = apiKeyTextStabilityApi.text.toString().trim()
                parentActivity.storeApiEndpoint(openAiEndpoint.text.toString().trim())
                parentActivity.openAiEndpoint  = openAiEndpoint.text.toString().trim()
                parentActivity.storeModel(openAiModel.text.toString().trim())
                parentActivity.openAiModel  = openAiModel.text.toString().trim()
                parentActivity.storeSystemMessage(systemMessage.text.toString().trim())
                parentActivity.systemMessage  = systemMessage.text.toString().trim()
            }else{

              //  Toast.makeText(requireActivity(),"Please enter your OpenAI key",Toast.LENGTH_SHORT).show()

            }
        }
        dialog.show()
    }

    fun stabilityChangeApiKey() {
        dialog = Dialog(requireActivity(),R.style.TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.stability_api_key_change)
        val doneButton = dialog.findViewById<LinearLayout>(R.id.doneButton)
        val apiKeyText = dialog.findViewById<EditText>(R.id.apiKeyText)
        val closeButton = dialog.findViewById<LinearLayout>(R.id.closeButton)
        val apiKeyOld =  parentActivity.getStoredStabilityApiKey()
        apiKeyText.setText(apiKeyOld)
        closeButton.setOnClickListener {
            dialog.dismiss()
            //gotoStabilityApi()
        }


        doneButton.setOnClickListener {
            val apiKeyValue = apiKeyText.text.toString().trim()
            if (apiKeyValue.isNotEmpty()){
                // API key not null
                dialog.dismiss()
                parentActivity.storeStabilityApiKey(apiKeyValue)
                parentActivity.stabilityApiKey  = apiKeyValue
            }else{

                //  Toast.makeText(requireActivity(),"Please enter your OpenAI key",Toast.LENGTH_SHORT).show()

            }
        }
        dialog.show()
    }

    fun scrollToBottom() {
        chatView.scrollToPosition(chatMessages.size-1)
    }

    private fun getResponse(question: String, callback: (String) -> Unit) {
        try {
            val url = "$openaiEndpoint/chat/completions"
            
            // Constructing the JSON body using org.json
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", openaiSystemMessage)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                })
            }
            
            val requestBody = JSONObject().apply {
                put("model", openaiModel)
                put("messages", messages)
                put("max_tokens", 500)
                put("temperature", 0.5)
                put("top_p", 0.9)
            }.toString()
    
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBody.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $openaiApiKey")
                .post(body)
                .build()
    
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("ChatGpt", "API request failed", e)
                    parentActivity.runOnUiThread {
                        sendChatGptResponse("Failed to get response: ${e.localizedMessage}", "err:")
                    }
                    callback("Failed to get response: ${e.localizedMessage}")
                }
    
                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { body ->
                        try {
                            val jsonObject = JSONObject(body)
                            if (jsonObject.has("choices")) {
                                val jsonArray = jsonObject.getJSONArray("choices")
                                val textResult = jsonArray.getJSONObject(0).getString("text").trim()
    
                                parentActivity.runOnUiThread {
                                    sendChatGptResponse(textResult, "res:")
                                }
                                callback(textResult)
                            } else {
                                val errorMsg = jsonObject.optJSONObject("error")?.getString("message")
                                    ?: "Unknown error occurred"
    
                                Log.e("ChatGpt", "Error in response: $errorMsg")
                                parentActivity.runOnUiThread {
                                    sendChatGptResponse(errorMsg, "err:")
                                }
                                callback(errorMsg)
                            }
                        } catch (e: JSONException) {
                            Log.e("ChatGpt", "JSON parsing error", e)
                            parentActivity.runOnUiThread {
                                sendChatGptResponse("Error parsing response: ${e.localizedMessage}", "err:")
                            }
                            callback("Error parsing response: ${e.localizedMessage}")
                        }
                    } ?: run {
                        Log.e("ChatGpt", "Empty response body")
                        parentActivity.runOnUiThread {
                            sendChatGptResponse("Received empty response", "err:")
                        }
                        callback("Received empty response")
                    }
                }
            })
        } catch (ex: Exception) {
            Log.e("ChatGpt", "getResponse exception", ex)
            parentActivity.runOnUiThread {
                sendChatGptResponse("Exception during request preparation: ${ex.localizedMessage}", "err:")
            }
            callback("Exception during request preparation: ${ex.localizedMessage}")
        }
    }
    
    

    override fun onUrlClick(position: Int, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onImageClick(position: Int, url: String, bitmap: Bitmap?) {
        val fullScreenPopup = FullScreenPopup(parentActivity, url,bitmap)
        fullScreenPopup.show()
    }

    override fun onStabilityApiClick(position: Int, chatModel: ChatModel) {
        gotoStabilityApi()
        stabilityChangeApiKey()
    }

    override fun onOpenApiClick(position: Int, chatModel: ChatModel) {
        openChangeApiKey()
    }


}