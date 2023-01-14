package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentJobListBinding
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

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

        binding.apply {
            composeJobList.setContent {
                val jobs by jobVM.jobs.observeAsState()
                CellsTheme {
                    JobList(jobs, Modifier)
                }
            }
        }

//        binding.jobList.layoutManager = LinearLayoutManager(requireActivity())
//        val adapter = JobListAdapter(this::onClicked)
//        binding.jobList.adapter = adapter
//        jobVM.jobs.observe(viewLifecycleOwner) {
//            adapter.submitList(it)
//        }
//        setupMenu()

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

@Composable
private fun JobList(
    jobs: List<RJob>?,
    modifier: Modifier = Modifier
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(jobs ?: listOf()) { job ->
            val jobTitle = "#${job.jobId}: ${job.label}"
            val jobProgress = (job.progress * 100).toFloat().div(job.total)
            JobListItem(
                title = jobTitle,
                status = getJobStatus(job),
                progress = jobProgress,
                modifier = modifier
            )
        }
    }
}

private fun getJobStatus(job: RJob?): String {
    if (job == null) {
        return "NaN"
    }

    var desc = "${job.status?.uppercase(Locale.getDefault())} "
    val createdTs = timestampToString(job.creationTimestamp, "dd-MM HH:mm:ss")
    val startTs = timestampToString(job.startTimestamp, "dd-MM HH:mm:ss")
    val updatedTs = timestampToString(job.updateTimestamp, "HH:mm:ss")
    val doneTs = timestampToString(job.doneTimestamp, "dd-MM HH:mm:ss")

    when {
        job.status == AppNames.JOB_STATUS_ERROR ||
                job.status == AppNames.JOB_STATUS_ERROR -> {
            desc += "at $doneTs: ${job.message}"
            // TODO re-introduce color management
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
// setTextColor(resources.getColor(R.color.danger, context.theme))
// }
        }
        job.status == AppNames.JOB_STATUS_CANCELLED -> {
            desc += "at $doneTs: ${job.message}"
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                setTextColor(resources.getColor(R.color.colorAccent, context.theme))
//            }
        }

        job.doneTimestamp > 0 -> {
            desc += "at $doneTs: ${job.message}"
        }
        job.startTimestamp > 0 -> {
            if (currentTimestamp() - job.updateTimestamp < 120) {
                desc += "${asSinceString(job.startTimestamp)}\n${job.progressMessage}"
            } else {
                desc += "idle ${asSinceString(job.updateTimestamp)}\nlast message: ${job.progressMessage}"
            }
        }
        else -> desc += " waiting since $createdTs"
    }
    return desc
}

@Composable
private fun JobListItem(
    title: String,
    status: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // TODO add rounded corner on the top right
    // TODO the progress bar part does not appear / disappear when needed.
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {
        Column(
            modifier = modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
            )
            if (progress > 0 && progress < 1) {
                LinearProgressIndicator(progress = progress)
            }
        }
    }
}

@Preview
@Composable
private fun JobListItemPreview(
) {
    CellsTheme {
        JobListItem("title", "status", 0.7f, Modifier)
    }
}
