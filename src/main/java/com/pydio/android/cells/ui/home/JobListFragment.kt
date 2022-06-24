package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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

        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_job_list, container, false
        )

        binding.jobList.layoutManager = LinearLayoutManager(requireActivity())
        val adapter = JobListAdapter(this::onClicked)
        binding.jobList.adapter = adapter
        jobVM.jobs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        return binding.root
    }

    private fun onClicked(node: RJob, command: String) {
        Log.i(logTag, "Clicked on ${node.jobId} -> $command")
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val clearListBtn = menu.findItem(R.id.clear_table)
        clearListBtn.isVisible = true
        clearListBtn.setOnMenuItemClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                jobVM.jobService.clearTerminated()
            }
            return@setOnMenuItemClickListener true
        }
   }

}
