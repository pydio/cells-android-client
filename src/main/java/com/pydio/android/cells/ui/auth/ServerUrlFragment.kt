package com.pydio.android.cells.ui.auth

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentServerUrlBinding
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.utils.hideKeyboard
import com.pydio.android.cells.utils.showLongMessage

/**
 * Handle the registration of a new Server URL, it manages:
 *  - URL validation
 *  - TLS check
 *  - Skip verify flag
 */
class ServerUrlFragment : Fragment() {

    private val logTag = ServerUrlFragment::class.simpleName

    private val serverUrlVM by sharedViewModel<ServerUrlViewModel>()
    private lateinit var binding: FragmentServerUrlBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_server_url, container, false
        )

        binding.actionButton.setOnClickListener { goForPing() }

        serverUrlVM.server.observe(viewLifecycleOwner) { server ->
            Log.i(logTag, "... Launching Authentication process")
            server?.let {
                val urlStr = server.serverURL.toJson()
                if (it.isLegacy) { // Navigate to in app legacy auth
                    val action = ServerUrlFragmentDirections.actionServerUrlToP8Creds(
                        urlStr,
                        AuthService.NEXT_ACTION_BROWSE
                    )
                    findNavController().navigate(action)
                    serverUrlVM.authLaunched()
                } else { // Launch OAuth Process
                    serverUrlVM.launchOAuthProcess(it.serverURL)
                }
            }
        }

        serverUrlVM.invalidTLS.observe(viewLifecycleOwner) { invalidTLS ->
            if (invalidTLS) {
                findNavController().navigate(ServerUrlFragmentDirections.actionConfirmSkipVerify())
            }
        }

        serverUrlVM.nextIntent.observe(viewLifecycleOwner) { intent ->
            intent?.let {
                startActivity(intent)
                serverUrlVM.intentStarted()
            }
        }

        serverUrlVM.isLoading.observe(viewLifecycleOwner) {
            binding.loadingBar.visibility = if (it) View.VISIBLE else View.GONE
            binding.urlEditText.isEnabled = !it
        }

        serverUrlVM.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { showLongMessage(this@ServerUrlFragment.requireContext(), msg) }
        }

        return binding.root
    }

    private fun goForPing() {
        serverUrlVM.pingAddress(binding.urlEditText.text.toString())
//        binding.apply {
//            // Update model?
//            // Important: trigger re-paint Really ?
//            invalidateAll()
//        }
        hideKeyboard()
    }
}

class ConfirmSkipTlsVerificationDialog : DialogFragment() {

    private val serverUrlVM by sharedViewModel<ServerUrlViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.confirm_skip_verify_title)
            .setMessage(R.string.confirm_skip_verify_desc)
            .setPositiveButton(R.string.button_i_understand_the_risks) { _, _ ->
                Log.i("ConfirmSkip", "Current stored address: ${serverUrlVM.serverAddress.value}")
                serverUrlVM.confirmTlsValidationSkip(true)
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                serverUrlVM.confirmTlsValidationSkip(false)
            }
        return builder.create()
    }
}
