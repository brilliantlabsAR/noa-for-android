import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import xyz.brilliant.argpt.R
import android.widget.Button
import android.widget.EditText


class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel
    val apiKeyEditText: EditText? = view?.findViewById(R.id.api_key_edit_text)
    val apiServerEditText: EditText? = view?.findViewById(R.id.api_server_edit_text)
    val systemMessageEditText: EditText? = view?.findViewById(R.id.system_message_edit_text)
    val modelEditText: EditText? = view?.findViewById(R.id.model_edit_text)
    val saveButton: Button? = view?.findViewById(R.id.save_button)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.settings_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        // Load the stored settings
        viewModel.loadSettings()

        // Observe the settings LiveData and update the UI
        viewModel.settings.observe(viewLifecycleOwner, Observer { settings: Settings ->
            apiKeyEditText?.setText(settings.apiKey)
            apiServerEditText?.setText(settings.apiServer)
            systemMessageEditText?.setText(settings.systemMessage)
            modelEditText?.setText(settings.model)
        })

        // Handle the save button click
        saveButton?.setOnClickListener {
            viewModel.saveSettings(
                apiKeyEditText?.text.toString(),
                apiServerEditText?.text.toString(),
                systemMessageEditText?.text.toString(),
                modelEditText?.text.toString()
            )
            dismiss()
        }
    }

    private fun dismiss() {
        parentFragmentManager.popBackStack()
    }
}