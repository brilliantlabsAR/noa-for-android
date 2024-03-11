package xyz.brilliant.argpt.helpers

import android.content.Context

class SharedPreferencesHelper(private val context: Context) {
    companion object {
        private const val PREFS_FILE_NAME = "MyPrefs"
        private const val PREFS_FILE_NAME2 = "ApiKey"
        private const val PREFS_KEY_DEVICE_ADDRESS = "DeviceAddress"
        private const val PREFS_OPEN_API_KEY = "OpenAi"
        private const val PREFS_STABILITY_API_KEY = "stability"
    }
    fun getStoredDeviceAddress(): String {
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_KEY_DEVICE_ADDRESS, "") ?: ""
    }
    fun getStoredAccessToken(): String {
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        return prefs.getString("token", "") ?: ""
    }

    fun getStoredApiKey(): String {
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME2, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_OPEN_API_KEY, "") ?: ""
    }

    fun getStoredStabilityApiKey(): String {
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME2, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_STABILITY_API_KEY, "") ?: ""
    }

    fun storeDeviceAddress(deviceAddress: String) {
        val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(PREFS_KEY_DEVICE_ADDRESS, deviceAddress)
        editor.apply()
    }

    fun getTokenFromSharedPreferences(): String {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("token", "") ?: ""
    }

    fun clearSharedPreferences() {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}

