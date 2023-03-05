package com.pydio.android.cells.ui.core.nav

import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.pydio.android.cells.ui.aaLegacy.box.account.AfterSuccessfulLogin
import com.pydio.android.cells.ui.aaLegacy.box.account.AskServerUrl
import com.pydio.android.cells.ui.aaLegacy.box.account.P8Credentials
import com.pydio.android.cells.ui.aaLegacy.box.account.ProcessAuth
import com.pydio.android.cells.ui.aaLegacy.box.account.SkipVerify
import com.pydio.android.cells.ui.models.LoginStep
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.cells.utils.Log
import kotlinx.coroutines.launch

private val logTag = "LoginNavGraph"

@Deprecated("Simple test still kept for a while for the pattern. Do not use.")
fun NavGraphBuilder.loginNavGraph(
    navController: NavController,
    loginVM: LoginVM,
    afterAuth: (Boolean) -> Unit,
    launchOAuth: (Intent) -> Unit,
) {

    navigation(
        startDestination = LoginStep.URL.name,
        route = CellsDestinations.Home.route
    ) {

        composable(LoginStep.URL.name) {

            Log.e(logTag, "Nav to Login Step")
            val scope = rememberCoroutineScope()
            val isProcessing = loginVM.isProcessing.collectAsState()
            val currAddress = loginVM.serverAddress.collectAsState()
            val message = loginVM.message.collectAsState()
            val errMsg = loginVM.errorMessage.collectAsState()

            val doPing: (String) -> Unit = { url ->
                // TODO add sanity checks
                scope.launch {
                    loginVM.pingAddress()
                }
            }

            AskServerUrl(
                isProcessing = isProcessing.value,
                message = message.value,
                errMsg = errMsg.value,
                urlString = currAddress.value,
                setUrl = { loginVM.setAddress(it) },
                pingUrl = { doPing(currAddress.value) },
                cancel = { afterAuth(false) }
            )

            loginVM.setCurrentStep(LoginStep.URL)
        }

        composable(LoginStep.SKIP_VERIFY.name) {

            val scope = rememberCoroutineScope()
            val isProcessing = loginVM.isProcessing.collectAsState()
            val currAddress = loginVM.serverAddress.collectAsState()
            val message = loginVM.message.collectAsState()
            val errMsg = loginVM.errorMessage.collectAsState()

            SkipVerify(
                isProcessing.value,
                currAddress.value,
                message.value,
                errMsg.value,
                goBack = { navController.navigateBack() },
                accept = { scope.launch { loginVM.confirmSkipVerifyAndPing() } },
            )
            loginVM.setCurrentStep(LoginStep.SKIP_VERIFY)
        }

        composable(LoginStep.P8_CRED.name) {

            val scope = rememberCoroutineScope()
            val isProcessing = loginVM.isProcessing.collectAsState()
            val message = loginVM.message.collectAsState()
            val errMsg = loginVM.errorMessage.collectAsState()

            val loginString = rememberSaveable { mutableStateOf("") }
            val updateLogin: (String) -> Unit = { loginString.value = it }
            val pwdString = rememberSaveable { mutableStateOf("") }
            val updatePwd: (String) -> Unit = { pwdString.value = it }

            val launchP8Auth: (String, String, String?) -> Unit = {
                // TODO add validation
                    login, pwd, captcha ->
                scope.launch { loginVM.logToP8(login, pwd, captcha) }
            }

            P8Credentials(
                isProcessing.value,
                loginString.value,
                updateLogin,
                pwdString.value,
                updatePwd,
                message = message.value,
                errMsg = errMsg.value,
                goBack = { navController.navigateBack() },
                launchP8Auth = launchP8Auth,
            )
            loginVM.setCurrentStep(LoginStep.P8_CRED)
        }

        composable(LoginStep.PROCESS_AUTH.name) {

            val isProcessing = loginVM.isProcessing.collectAsState()
            val message = loginVM.message.collectAsState()

            ProcessAuth(isProcessing.value, message.value)
            loginVM.setCurrentStep(LoginStep.PROCESS_AUTH)
        }

        composable(LoginStep.DONE.name) {
            // This page should never be seen.
            AfterSuccessfulLogin()
            loginVM.setCurrentStep(LoginStep.DONE)
        }

//        composable("username") { ... }
//        composable("password") { ... }
//        composable("registration") { ... }
    }

//    NavHost(
//        navController = navController,
//        startDestination = LoginStep.URL.name,
//    ) {
//
//
//    }

}

// Kind of hack to be able to navigate back using the navController
// TODO clean this
private fun NavController.navigateBack() {
    if (backQueue.size > 2) {
        popBackStack()
    }
}
