package com.pydio.android.cells.ui.aaLegacy.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.LoginActivity
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentDefaultBinding

/**
 * Default fragment that is shown when no other better choice is found.
 * Presents the user with the option to create a first account.
 */
class DefaultFragment : Fragment(), AppNames {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding: FragmentDefaultBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_default, container, false
        )

        binding.addAccountButton.setOnClickListener {
            // Launch the account activity with a new intent
            val toAuthIntent = Intent(requireActivity(), LoginActivity::class.java)
            startActivity(toAuthIntent)
        }

        return binding.root
    }
}
