import android.content.Context
import android.content.SharedPreferences

class SettingsViewModel(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SettingsPreferences", Context.MODE_PRIVATE)

    fun saveSettings(apiKey: String, apiServer: String, systemMessage: String, model: String) {
        sharedPreferences.edit()
            .putString("api_key", apiKey)
            .putString("api_server", apiServer)
            .putString("system_message", systemMessage)
            .putString("model", model)
            .apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString("api_key", "") ?: ""
    }

    fun getApiServer(): String {
        return sharedPreferences.getString("api_server", "") ?: ""
    }

    fun getSystemMessage(): String {
        return sharedPreferences.getString("system_message", "") ?: ""
    }

    fun getModel(): String {
        return sharedPreferences.getString("model", "") ?: ""
    }
}