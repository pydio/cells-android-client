package com.pydio.android.cells.ui.transferxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pydio.cells.transport.StateID
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentPickSessionBinding

class PickSessionFragment : Fragment() {

    // private val logTag = PickSessionFragment::class.java.simpleName

    private val chooseTargetVM: ChooseTargetViewModel by sharedViewModel()
    private val targetSessionVM: PickSessionViewModel by viewModel()
    private lateinit var binding: FragmentPickSessionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_pick_session, container, false
        )
        setHasOptionsMenu(true)

        val adapter = SessionListAdapter { stateID, action -> onClicked(stateID, action) }
        binding.sessions.adapter = adapter
        targetSessionVM.sessions.observe(viewLifecycleOwner) { adapter.submitList(it) }

        return binding.root
    }

    private fun onClicked(stateID: StateID, command: String) {
        when (command) {
            AppNames.ACTION_OPEN -> {
                val action = PickSessionFragmentDirections.actionPickFolder(stateID.id)
                findNavController().navigate(action)
            }
            else -> return // do nothing
        }
    }

    override fun onResume() {
        super.onResume()
        chooseTargetVM.setCurrentState(null)

        (requireActivity() as AppCompatActivity).supportActionBar?.let { bar ->
            bar.setDisplayHomeAsUpEnabled(false)
            bar.title = when (chooseTargetVM.actionContext){
                AppNames.ACTION_UPLOAD -> resources.getString(R.string.choose_target_for_share_title)
                AppNames.ACTION_COPY -> resources.getString(R.string.choose_target_for_copy_title)
                AppNames.ACTION_MOVE -> resources.getString(R.string.choose_target_for_move_title)
                else -> resources.getString(R.string.choose_target_subtitle)
            }
            bar.subtitle = resources.getString(R.string.choose_target_subtitle)
        }
    }
}
