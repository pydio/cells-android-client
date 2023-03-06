package com.pydio.android.cells.ui.aaLegacy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentJobListBinding
import com.pydio.android.cells.ui.system.models.JobListVM
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class JobListFragment : Fragment() {

    //private val logTag = JobListFragment::class.java.simpleName
    private val jobVM: JobListVM by viewModel()
    private lateinit var binding: FragmentJobListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_job_list, container, false
        )
        binding.apply {
            composeJobList.setContent {
                val jobs by jobVM.jobs.observeAsState()
                CellsTheme {
                 //    JobList(jobs ?: listOf())
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
                            jobVM.jobService.clearTerminated()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
