package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentLogListBinding
import com.pydio.android.cells.ui.box.system.LogList
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LogListFragment : Fragment() {

    // private val logTag = LogListFragment::class.java.simpleName
    private val logListVM: LogListViewModel by viewModel()
    private lateinit var binding: FragmentLogListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_log_list, container, false
        )
        binding.apply {
            composeLogList.setContent {
                val logs by logListVM.logs.observeAsState()
                CellsTheme {
                    LogList(logs, Modifier)
                }
            }
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
                return when (menuItem.itemId) {
                    R.id.clear_table -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            logListVM.jobService.clearAllLogs()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}

