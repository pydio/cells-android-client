package com.pydio.android.cells.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pydio.android.cells.databinding.ZzSampleWidgetsBinding

class SampleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val binding = ZzSampleWidgetsBinding.inflate(inflater, container, false)
        return binding.root
    }
}
