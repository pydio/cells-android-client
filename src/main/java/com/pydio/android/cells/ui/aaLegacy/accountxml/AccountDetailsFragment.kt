package com.pydio.android.cells.ui.aaLegacy.accountxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAccountDetailsBinding

class AccountDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: FragmentAccountDetailsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_account_details, container, false
        )

        return binding.root
    }
}
