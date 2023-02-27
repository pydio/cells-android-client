package com.pydio.android.cells.ui.aaLegacy.box

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.aaLegacy.box.account.AfterSuccessfulLogin
import com.pydio.android.cells.ui.aaLegacy.box.account.AskServerUrl
import com.pydio.android.cells.ui.aaLegacy.box.account.P8Credentials
import com.pydio.android.cells.ui.aaLegacy.box.account.ProcessAuth
import com.pydio.android.cells.ui.aaLegacy.box.account.SkipVerify
import com.pydio.android.cells.ui.models.LoginStep
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.launch

private const val logTag = "AuthScreen.kt"

private sealed class AuthDestinations(val route: LoginStep) {
    object ServerURL : AuthDestinations(LoginStep.URL)
    object CertNotValid : AuthDestinations(LoginStep.SKIP_VERIFY)
    object P8Credentials : AuthDestinations(LoginStep.P8_CRED)
    object Processing : AuthDestinations(LoginStep.PROCESS_AUTH)
}

@Composable
fun AuthHost(
    navController: NavHostController,
    loginVM: LoginVM,
    afterAuth: (Boolean) -> Unit,
    launchOAuth: (Intent) -> Unit,
) {

    LaunchedEffect(true) {
        loginVM.currDestination.collect {
            if (it.name != navController.currentDestination?.route) {
                Log.d(logTag, "Curr dest was: ${navController.currentDestination?.route}")
                Log.d(logTag, "New step is: $it")
                if (it == LoginStep.DONE) {
                    // we don't need to show the "done": Auth has already been done and processed,
                    // and showing the page adds some useless "blinking".
                    afterAuth(true)
                } else {
                    navController.navigate(it.name)
                }
            }
        }
    }

    LaunchedEffect(true) {
        loginVM.oauthIntent.collect {
            if (it != null) {
                launchOAuth(it)
            }
        }
    }

    val scope = rememberCoroutineScope()
    val currAddress = loginVM.serverAddress.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()
    val isProcessing = loginVM.isProcessing.collectAsState()

    val doPing: (String) -> Unit = { url ->
        // TODO add sanity checks
        scope.launch {
            loginVM.pingAddress()
        }
    }


    // TODO nice linear progress indicator with animated steps that are bound to the current page
    // LinearProgressIndicator()

    NavHost(
        navController = navController,
        startDestination = LoginStep.URL.name,
    ) {

        composable(LoginStep.URL.name) {

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

            ProcessAuth(isProcessing.value, message.value)
            loginVM.setCurrentStep(LoginStep.PROCESS_AUTH)
        }

        composable(LoginStep.DONE.name) {
            // This page should never be seen.
            AfterSuccessfulLogin()
            loginVM.setCurrentStep(LoginStep.DONE)
        }
    }
}

@Composable
fun AuthApp(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

// Kind of hack to be able to navigate back using the navController
// TODO clean this
private fun NavController.navigateBack() {
    if (backQueue.size > 2) {
        popBackStack()
    }
}