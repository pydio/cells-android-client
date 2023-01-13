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
import com.pydio.android.cells.databinding.FragmentJobListBinding
import com.pydio.android.cells.db.runtime.RJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class JobListFragment : Fragment() {

    private val logTag = JobListFragment::class.java.simpleName
    private val jobVM: JobListViewModel by viewModel()
    private lateinit var binding: FragmentJobListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_job_list, container, false
        )
//        binding.apply {
//            composeJobList.setContent {
//                MaterialTheme {
//                    JobList()
//                }
//            }
//        }

        binding.jobList.layoutManager = LinearLayoutManager(requireActivity())
        val adapter = JobListAdapter(this::onClicked)
        binding.jobList.adapter = adapter
        jobVM.jobs.observe(viewLifecycleOwner) {
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
                            jobVM.jobService.clearTerminated()
                        }
                        return true
                    }
                    else -> return false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onClicked(node: RJob, command: String) {
        Log.d(logTag, "Clicked on ${node.jobId} -> $command")
    }
}

//@Composable
//fun JobList() {
//    Surface {
//        Text("Hello Compose")
//    }
//}


