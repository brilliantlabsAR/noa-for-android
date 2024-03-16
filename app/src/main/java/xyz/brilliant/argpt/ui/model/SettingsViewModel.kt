import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel(context: Context) : ViewModel() {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SettingsPreferences", Context.MODE_PRIVATE)
    
    private val _settings = MutableLiveData<Settings>()
    val settings: LiveData<Settings>
        get() = _settings

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

    fun loadSettings() {
        val settings = Settings(
            getApiKey(),
            getApiServer(),
            getSystemMessage(),
            getModel()
        )
        _settings.value = settings
    }
    fun getSettings(): Settings {
        return _settings.value ?: Settings("", "", "", "")
    }
}