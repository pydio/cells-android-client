package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentLogListBinding
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.timestampToString
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

        binding.apply {
            composeLogList.setContent {
                val logs by logListVM.logs.observeAsState()
                CellsTheme {
                    LogList(logs, Modifier)
                }
            }
        }

//        binding.logList.layoutManager = LinearLayoutManager(activity)
//        val adapter = LogListAdapter(this::onClicked)
//        binding.logList.adapter = adapter
//        logListVM.logs.observe(viewLifecycleOwner) {
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

@Composable
private fun LogList(
    logs: List<RLog>?,
    modifier: Modifier = Modifier
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(logs ?: listOf()) { log ->
            LogListItem(
                title = getTitle(log),
                message = log.message ?: "",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LogListItem(
    title: String,
    message: String,
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun LogListItemPreview(
) {
    CellsTheme {
        LogListItem("title", "status", Modifier)
    }
}


//@Composable
//private fun LogTitle(item: RLog?) {
private fun getTitle(item: RLog): String {
    val ts = timestampToString(item.timestamp, "dd-MM HH:mm:ss")
    val level = item.getLevelString()
    return "[$level] $ts - Job #${item.callerId}"

    // TODO
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            when (item.level) {
//                1, 2 -> setTextColor(resources.getColor(R.color.danger, context.theme))
//                3 -> setTextColor(resources.getColor(R.color.colorAccent, context.theme))
//                4 -> setTextColor(resources.getColor(R.color.ok, context.theme))
//                else -> setTextColor(resources.getColor(R.color.material_neutral, context.theme))
//            }
//        }
}
