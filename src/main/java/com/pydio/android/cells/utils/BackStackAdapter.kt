package com.pydio.android.cells.utils

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.CellsApp

/**
 * Tweak back navigation to force target state to be the root of the account when navigating back
 * from the first level, typically, workspace root, bookmarks or accounts, otherwise we will be
 * redirected back to where we are by the logic that is launched base on the state when the
 * workspace list fragment resumes.
 */
class BackStackAdapter(enabled: Boolean = false) : OnBackPressedCallback(enabled) {

    private var accountID: StateID? = null
    private lateinit var manager: FragmentManager
    private lateinit var navController: NavController

    fun initializeBackNavigation(
        manager: FragmentManager,
        navController: NavController,
        stateID: StateID
    ) {
        this.manager = manager
        this.navController = navController
        if (stateID.isWorkspaceRoot) {
            isEnabled = true
            accountID = StateID.fromId(stateID.accountId)
        }
    }

    override fun handleOnBackPressed() {
        accountID?.let {
            Log.i("BackStackAdapter", "Setting custom state before navigating back")
//            CellsApp.instance.setCurrentState(it)
            navController.navigateUp()
        }
    }

    companion object {

        fun initialised(
            manager: FragmentManager,
            navController: NavController,
            stateID: StateID
        ): BackStackAdapter {
            val backPressedCallback = BackStackAdapter()
            backPressedCallback.initializeBackNavigation(manager, navController, stateID)
            return backPressedCallback
        }
    }
}