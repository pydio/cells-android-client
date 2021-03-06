package com.pydio.android.cells.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.MainActivity
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentP8CredentialsBinding
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Handle user filled credentials for P8 remote servers */
class P8CredentialsFragment : Fragment() {

    private val logTag = P8CredentialsFragment::class.simpleName

    private val p8CredVM: P8CredViewModel by viewModel()
    private lateinit var binding: FragmentP8CredentialsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_p8_credentials, container, false
        )
        val credArgs by navArgs<P8CredentialsFragmentArgs>()

        p8CredVM.setUrl(ServerURLImpl.fromJson(credArgs.serverUrlString))
        binding.p8CredViewModel = p8CredVM
        binding.lifecycleOwner = this

        binding.actionButton.setOnClickListener { launchAuth() }
        binding.cancelButton.setOnClickListener { p8CredVM.cancel() }

        p8CredVM.accountID.observe(viewLifecycleOwner) { accountId ->
            accountId?.let {
                when (credArgs.nextAction) {
                    AuthService.NEXT_ACTION_ACCOUNTS,
                    AuthService.NEXT_ACTION_TERMINATE -> {} // Do nothing => we return where we launched the auth process
                    AuthService.NEXT_ACTION_BROWSE -> {
                        // We have registered a new account and want to browse to it
                        val nextState = StateID.fromId(it)
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        intent.putExtra(AppKeys.EXTRA_STATE, it)
                        Log.i(logTag, "Auth success, about to browse to $nextState")
                        startActivity(intent)
                    }
                    else -> Log.i(
                        logTag, "Auth successful but invalid next action: ${credArgs.nextAction}"
                    )
                }
                requireActivity().finish()
            }
        }
        p8CredVM.isProcessing.observe(viewLifecycleOwner) {
            binding.loadingBar.visibility = if (it) View.VISIBLE else View.GONE
            binding.actionButton.isEnabled = !it
            binding.loginEditText.isEnabled = !it
            binding.passwordEditText.isEnabled = !it
            binding.captchaEditText.isEnabled = !it
            binding.executePendingBindings()
        }

        p8CredVM.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                showLongMessage(requireContext(), it)
            }
        }

        // TODO handle captcha
        return binding.root
    }

    private fun launchAuth() {
        p8CredVM.logToP8(
            binding.loginEditText.text.toString(),
            binding.passwordEditText.text.toString(),
            null
        )
    }
}
