package com.pydio.android.cells.ui.login.aaLegacy

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

// TODO double check
private const val prefix = "login/"

object RouteLoginDone {
    const val route = "login/done"
}

//object RouteLoginStarting : LoginNavRoute<StateViewModel> {
//    override val route = "${prefix}starting"
//
//    @Composable
//    override fun viewModel(): StateViewModel = koinViewModel()
//
//    @Composable
//    override fun Content(
//        viewModel: StateViewModel,
//        loginVM: LoginViewModelNew,
//        navigateTo: (String?) -> Unit,
//    ) = AskServerUrl(viewModel, loginVM, navigateTo)
//}
//
//object RouteLoginUrl : LoginNavRoute<StateViewModel> {
//
//    override val route = "${prefix}url"
//
//    @Composable
//    override fun viewModel(): StateViewModel = koinViewModel()
//
//    @Composable
//    override fun Content(
//        viewModel: StateViewModel,
//        loginVM: LoginViewModelNew,
//        navigateTo: (String?) -> Unit,
//    ) = AskServerUrl(viewModel, loginVM, navigateTo)
//}
//
//object RouteLoginSkipVerify : LoginNavRoute<StateViewModel> {
//
//    override val route = "${prefix}skip-verify"
//
//    @Composable
//    override fun viewModel(): StateViewModel = koinViewModel()
//
//    @Composable
//    override fun Content(
//        viewModel: StateViewModel,
//        loginVM: LoginViewModelNew,
//        navigateTo: (String?) -> Unit,
//    ) = SkipVerify(viewModel, loginVM, navigateTo)
//}
//
//object RouteLoginP8Credentials : LoginNavRoute<StateViewModel> {
//
//    override val route = "${prefix}p8-credentials"
//
//    @Composable
//    override fun viewModel(): StateViewModel = koinViewModel()
//
//    @Composable
//    override fun Content(
//        viewModel: StateViewModel,
//        loginVM: LoginViewModelNew,
//        navigateTo: (String?) -> Unit
//    ) = P8Credentials(viewModel, loginVM, navigateTo)
//}
//
//object RouteLoginProcessAuth : LoginNavRoute<StateViewModel> {
//
//    override val route = "${prefix}process-auth"
//
//    @Composable
//    override fun viewModel(): StateViewModel = koinViewModel()
//
//    @Composable
//    override fun Content(
//        viewModel: StateViewModel,
//        loginVM: LoginViewModelNew,
//        navigateTo: (String?) -> Unit
//    ) = ProcessAuth(viewModel, loginVM, navigateTo)
//}
//
