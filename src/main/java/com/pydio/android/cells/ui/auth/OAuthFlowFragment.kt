package com.pydio.android.cells.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.MainActivity
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentOauthFlowBinding
import com.pydio.android.cells.services.AuthService
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Manages the external OAuth process to get a JWT Token */
class OAuthFlowFragment : Fragment() {

    private val logTag = OAuthFlowFragment::class.simpleName

    private val oAuthVM: OAuthViewModel by viewModel()
    private lateinit var binding: FragmentOauthFlowBinding
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_oauth_flow, container, false
        )

        binding.oAuthViewModel = oAuthVM
        navController = findNavController()

        oAuthVM.accountID.observe(viewLifecycleOwner) { pair ->
            pair?.let {
                val (accountID, next) = pair
                when (next) {
                    AuthService.NEXT_ACTION_ACCOUNTS,
                    AuthService.NEXT_ACTION_TERMINATE -> {
                    } // Do nothing => we return where we launched the auth process
                    AuthService.NEXT_ACTION_BROWSE -> {
                        // We have registered a new account and want to browse to it
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        intent.putExtra(AppKeys.EXTRA_STATE, accountID)
                        Log.i(logTag, "Auth Successful, navigating to ${StateID.fromId(accountID)}")
                        startActivity(intent)
                    }
                }
                requireActivity().finish()
            }
        }

        binding.actionButton.setOnClickListener { navController.navigateUp() }

        return binding.root
    }

    override fun onResume() {
        Log.i(logTag, "onResume")
        super.onResume()
        val uri = requireActivity().intent.data ?: return
        val state = uri.getQueryParameter(AppNames.QUERY_KEY_STATE)
        val code = uri.getQueryParameter(AppNames.QUERY_KEY_CODE)
        if (code != null && state != null) {
            oAuthVM.handleResponse(state, code)
        }
    }

    override fun onPause() {
        Log.i(logTag, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.i(logTag, "onStop")
        super.onStop()
    }
}
