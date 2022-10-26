package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentLogListBinding
import com.pydio.android.cells.db.runtime.RLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LogListFragment : Fragment() {

    private val logTag = LogListFragment::class.java.simpleName
    private val logListVM: LogListViewModel by viewModel()
    private lateinit var binding: FragmentLogListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_log_list, container, false
        )

        binding.logList.layoutManager = LinearLayoutManager(activity)
        val adapter = LogListAdapter(this::onClicked)
        binding.logList.adapter = adapter
        logListVM.logs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        setupMenu()

        return binding.root
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {

            override fun onPrepareMenu(menu: Menu) {}

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.table_listing_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.clear_table -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            logListVM.jobService.clearAllLogs()
                        }
                        return true
                    }
                    else -> return false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun onClicked(node: RLog, command: String) {
        Log.i(logTag, "Clicked on ${node.logId} -> $command")
//        when (command) {
//            // AppNames.ACTION_OPEN -> navigateTo(node)
//            AppNames.ACTION_MORE -> {
//                val action = TransferListFragmentDirections.openTransferMenu(
//                    node.encodedState,
//                    node.transferId
//                )
//                findNavController().navigate(action)
//            }
//            else -> return // do nothing
//        }
    }

}
