import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SettingsPreferences", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "api_key"
        private const val API_SERVER = "api_server"
        private const val SYSTEM_MESSAGE = "system_message"
        private const val MODEL = "model"

        @Volatile
        private var instance: PreferencesHelper? = null

        fun getInstance(context: Context): PreferencesHelper {
            return instance ?: synchronized(this) {
                instance ?: PreferencesHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(API_KEY, apiKey).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(API_KEY, "") ?: ""
    }

    fun saveApiServer(apiServer: String) {
        sharedPreferences.edit().putString(API_SERVER, apiServer).apply()
    }

    fun getApiServer(): String {
        return sharedPreferences.getString(API_SERVER, "") ?: ""
    }

    fun saveSystemMessage(systemMessage: String) {
        sharedPreferences.edit().putString(SYSTEM_MESSAGE, systemMessage).apply()
    }

    fun getSystemMessage(): String {
        return sharedPreferences.getString(SYSTEM_MESSAGE, "") ?: ""
    }

    fun saveModel(model: String) {
        sharedPreferences.edit().putString(MODEL, model).apply()
    }

    fun getModel(): String {
        return sharedPreferences.getString(MODEL, "") ?: ""
    }
}