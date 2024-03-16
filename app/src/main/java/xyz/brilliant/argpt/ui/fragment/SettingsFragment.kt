import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.settings_popup.*
import xyz.brilliant.argpt.R

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.settings_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        // Load the stored settings
        viewModel.loadSettings()

        // Observe the settings LiveData and update the UI
        viewModel.settings.observe(viewLifecycleOwner, { settings ->
            api_key_edit_text.setText(settings.apiKey)
            api_server_edit_text.setText(settings.apiServer)
            system_message_edit_text.setText(settings.systemMessage)
            model_edit_text.setText(settings.model)
        })

        // Handle the save button click
        save_button.setOnClickListener {
            val apiKey = api_key_edit_text.text.toString()
            val apiServer = api_server_edit_text.text.toString()
            val systemMessage = system_message_edit_text.text.toString()
            val model = model_edit_text.text.toString()
            viewModel.saveSettings(apiKey, apiServer, systemMessage, model)
            dismiss()
        }
    }

    private fun dismiss() {
        parentFragmentManager.popBackStack()
    }
}