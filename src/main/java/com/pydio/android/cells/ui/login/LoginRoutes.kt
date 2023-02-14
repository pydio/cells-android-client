package com.pydio.android.cells.ui.login

import androidx.compose.runtime.Composable
import com.pydio.android.cells.ui.login.nav.StateViewModel
import com.pydio.android.cells.ui.login.screens.AskServerUrl
import com.pydio.android.cells.ui.login.screens.P8Credentials
import com.pydio.android.cells.ui.login.screens.ProcessAuth
import com.pydio.android.cells.ui.login.screens.SkipVerify
import org.koin.androidx.compose.koinViewModel


/**
 * Declare all routes for the Login process
 */

object DoneRoute {
    val route = "done"
}

object UrlRouteLogin : LoginNavRoute<StateViewModel> {

    override val route = "url"

    @Composable
    override fun viewModel(): StateViewModel = koinViewModel()

    @Composable
    override fun Content(
        viewModel: StateViewModel,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit,
    ) = AskServerUrl(viewModel, loginVM, navigateTo)
}

object SkipVerifyRouteLogin : LoginNavRoute<StateViewModel> {

    override val route = "skip-verify"

    @Composable
    override fun viewModel(): StateViewModel = koinViewModel()

    @Composable
    override fun Content(
        viewModel: StateViewModel,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit,
    ) = SkipVerify(viewModel, loginVM, navigateTo)
}

object P8CredsRouteLogin : LoginNavRoute<StateViewModel> {

    override val route = "p8-credentials"

    @Composable
    override fun viewModel(): StateViewModel = koinViewModel()

    @Composable
    override fun Content(
        viewModel: StateViewModel,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit
    ) = P8Credentials(viewModel, loginVM, navigateTo)
}

object ProcessAuthRouteLogin : LoginNavRoute<StateViewModel> {

    override val route = "process-auth"

    @Composable
    override fun viewModel(): StateViewModel = koinViewModel()

    @Composable
    override fun Content(
        viewModel: StateViewModel,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit
    ) = ProcessAuth(viewModel, loginVM, navigateTo)
}

