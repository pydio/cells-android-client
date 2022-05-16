package com.pydio.android.cells.ui.account

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.AuthActivity
import com.pydio.android.cells.MainActivity
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAccountListBinding
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.tasks.loginAccount
import com.pydio.android.cells.ui.common.deleteAccount
import com.pydio.android.cells.ui.common.logoutAccount
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Holds a list with the defined accounts and their status to switch between accounts
 * and log in and out.
 * Account details is not yet implemented
 */
class AccountListFragment : Fragment() {

    private val logTag = AccountListFragment::class.java.simpleName

    private lateinit var binding: FragmentAccountListBinding

    private val authService: AuthService by inject()
    private val sessionFactory: SessionFactory by inject()
    private val accountService: AccountService by inject()

    private val accountListVM: AccountListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(logTag, "onCreateView ${savedInstanceState?.getString(AppNames.EXTRA_STATE)}")
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account_list, container, false
        )

        val adapter = AccountListAdapter { accountID, action ->
            onAccountClicked(accountID, action)
        }
        binding.accountList.adapter = adapter

        accountListVM.sessions.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyContent.visibility = View.VISIBLE
                binding.accountList.visibility = View.GONE
            } else {
                binding.accountList.visibility = View.VISIBLE
                binding.emptyContent.visibility = View.GONE
                adapter.submitList(it)
            }
        }

        binding.newAccountFab.setOnClickListener {
            val toAuthIntent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(toAuthIntent)
        }

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun onAccountClicked(accountID: String, action: String) {
        Log.i(logTag, "ID: $accountID, do $action")
        when (action) {
            AppNames.ACTION_LOGIN -> {
                val currSession = accountListVM.sessions.value
                    ?.filter { it.accountID == accountID }
                    ?.get(0)
                if (currSession == null) {
                    Log.i(logTag, "No live session found for: $accountID in ViewModel, aborting...")
                    return
                }
                loginAccount(
                    requireContext(),
                    authService,
                    sessionFactory,
                    currSession,
                    AuthService.NEXT_ACTION_ACCOUNTS
                )
            }
            AppNames.ACTION_LOGOUT -> lifecycleScope.launch {
                logoutAccount(requireContext(), accountID, accountService)
            }
            AppNames.ACTION_FORGET -> {
                deleteAccount(requireContext(), accountID, accountService)
            }
            AppNames.ACTION_OPEN -> lifecycleScope.launch {
                accountService.openSession(accountID)
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.putExtra(AppNames.EXTRA_STATE, accountID)
                startActivity(intent)
            }
            else -> return // do nothing
        }
    }

    override fun onResume() {
        super.onResume()
        accountListVM.resume(true)
    }

    override fun onPause() {
        super.onPause()
        accountListVM.pause()
    }
}
