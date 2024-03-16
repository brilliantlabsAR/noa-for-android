import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import xyz.brilliant.argpt.preferences.PreferencesHelper

class SettingsViewModel : ViewModel() {

    private val preferencesHelper = PreferencesHelper.getInstance()

    private val _settings = MutableLiveData<Settings>()
    val settings: LiveData<Settings> = _settings

    fun loadSettings() {
        val apiKey = preferencesHelper.getApiKey()
        val apiServer = preferencesHelper.getApiServer()
        val systemMessage = preferencesHelper.getSystemMessage()
        val model = preferencesHelper.getModel()
        _settings.value = Settings(apiKey, apiServer, systemMessage, model)
    }

    fun saveSettings(apiKey: String, apiServer: String, systemMessage: String, model: String) {
        preferencesHelper.saveApiKey(apiKey)
        preferencesHelper.saveApiServer(apiServer)
        preferencesHelper.saveSystemMessage(systemMessage)
        preferencesHelper.saveModel(model)
    }
}

data class Settings(
    val apiKey: String,
    val apiServer: String,
    val systemMessage: String,
    val model: String
)