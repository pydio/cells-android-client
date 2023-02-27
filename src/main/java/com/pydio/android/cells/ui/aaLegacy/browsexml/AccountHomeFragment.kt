package com.pydio.android.cells.ui.aaLegacy.browsexml

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.LoginActivity
import com.pydio.android.cells.MainNavDirections
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAccountHomeBinding
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Displays the landing page for a given account, mainly the workspace list.
 * This is the default entry point for the MainActivity.
 */
class AccountHomeFragment : Fragment() {

    private val logTag = AccountHomeFragment::class.simpleName
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()
    private lateinit var binding: FragmentAccountHomeBinding

    private val accountDao: AccountDao by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(logTag, "onCreateView ${activeSessionVM.accountId}")

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account_home, container, false
        )

        return binding.root
    }

    override fun onResume() {
        Log.d(logTag, "onResume: ${activeSessionVM.accountId}")
        super.onResume()

        lifecycleScope.launch {
            // Handle corner case when current session has been deleted
            activeSessionVM.accountId?.let {
                withContext(Dispatchers.IO) { accountDao.getAccount(it) }
            } ?: let {
                activeSessionVM.afterCreate(null)
                binding.noAccountHome.visibility = View.VISIBLE
                binding.accountHomeHeader.visibility = View.GONE
                binding.forceRefresh.visibility = View.GONE
                binding.emptyContent.visibility = View.GONE
                configureActionButton()
                (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
                    bar.title = resources.getString(R.string.application_title)
                }
            }
        }

        if (activeSessionVM.accountId != null) {
            binding.noAccountHome.visibility = View.GONE
            binding.accountHomeHeader.visibility = View.VISIBLE
            binding.forceRefresh.visibility = View.VISIBLE
            binding.forceRefresh.setOnRefreshListener { activeSessionVM.forceRefresh() }
            binding.switchAccountButton.setOnClickListener {
                findNavController().navigate(MainNavDirections.openAccountList())
            }
            activeSessionVM.isLoading.observe(viewLifecycleOwner) {
                binding.forceRefresh.isRefreshing = it
            }
            activeSessionVM.errorMessage.observe(viewLifecycleOwner) { msg ->
                msg?.let { showLongMessage(requireContext(), msg) }
            }
            activeSessionVM.workspaces.observe(viewLifecycleOwner) {
                it?.let {
                    val adapter = WorkspaceListAdapter { slug, action -> onWsClicked(slug, action) }

                    binding.workspaces.adapter = adapter
                    val columns = resources.getInteger(R.integer.grid_ws_column_number)
                    val manager = GridLayoutManager(requireContext(), columns)
                    manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int) =
                            when (adapter.getItemViewType(position)) {
                                AppNames.ITEM_TYPE_HEADER -> columns
                                else -> 1
                            }
                    }
                    binding.workspaces.layoutManager = manager

                    if (it.isEmpty()) {
                        binding.emptyContent.visibility = View.VISIBLE
                        binding.workspaces.visibility = View.GONE
                    } else {
                        binding.workspaces.visibility = View.VISIBLE
                        binding.emptyContent.visibility = View.GONE
                        adapter.addHeaderAndSubmitList(it)
                    }
                }
            }

            activeSessionVM.sessionView.observe(viewLifecycleOwner) {
                it?.let { sessionView ->
                    (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
                        bar.title = sessionView.serverLabel() ?: sessionView.url
                    }

                    binding.session = sessionView
                }
            }
            activeSessionVM.resume()
        }
    }


    private suspend fun configureActionButton() {
        val accounts = withContext(Dispatchers.IO) { accountDao.getAccounts() }
        when (accounts.size) {
            0 -> { // No account: launch registration
                binding.noAccountButton.text =
                    resources.getText(R.string.welcome_add_account_button)
                binding.noAccountButton.setOnClickListener {
                    startActivity(Intent(requireActivity(), LoginActivity::class.java))
                }
            }
            1 -> { // Launch browsing directly
                binding.noAccountButton.text = resources.getText(R.string.action_browse)
                binding.noAccountButton.setOnClickListener {
                    val action = MainNavDirections.openAccountHome(accounts[0].accountID)
                    findNavController().navigate(action)
                }
            }
            // else we suggest to open the account list
            else -> {
                binding.noAccountButton.text = resources.getText(R.string.action_choose_account)
                binding.noAccountButton.setOnClickListener {
                    findNavController().navigate(MainNavDirections.openAccountList())
                }
            }
        }
    }

    private fun onWsClicked(slug: String, command: String) {
        val activeSession = activeSessionVM.sessionView.value ?: return
        when (command) {
            AppNames.ACTION_OPEN -> {
                val targetState = StateID.fromId(activeSession.accountID).withPath("/${slug}")
                findNavController().navigate(MainNavDirections.openFolder(targetState.id))
            }
            else -> return // do nothing
        }
    }

    override fun onPause() {
        val idStr = activeSessionVM.sessionView.value?.accountID ?: "No active session"
        Log.d(logTag, "Pausing: $idStr")
        super.onPause()
        activeSessionVM.pause()
    }
}
