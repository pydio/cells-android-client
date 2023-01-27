package com.pydio.android.cells.ui.box

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.box.account.AskServerUrl
import com.pydio.android.cells.ui.box.account.P8Credentials
import com.pydio.android.cells.ui.box.account.ProcessAuth
import com.pydio.android.cells.ui.box.account.SkipVerify
import com.pydio.android.cells.ui.models.LoginStep
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.theme.danger
import com.pydio.cells.utils.Str

private const val logTag = "AuthScreen.kt"

private sealed class AuthDestinations(val route: LoginStep) {
    object ServerURL : AuthDestinations(LoginStep.URL)
    object CertNotValid : AuthDestinations(LoginStep.SKIP_VERIFY)
    object P8Credentials : AuthDestinations(LoginStep.P8_CRED)
    object Processing : AuthDestinations(LoginStep.OAUTH_FLOW)
}

@Composable
fun AuthHost(
    navController: NavHostController,
    loginVM: LoginVM,
    afterAuth: (Boolean) -> Unit,
) {

    val ctx = LocalContext.current

    LaunchedEffect(true) {
        loginVM.currDestination.collect {
            Log.e(logTag, "New step received: $it")
            if (it.name != navController.currentDestination?.route) {
                Log.e(logTag, "Curr dest was: ${navController.currentDestination?.route}")
                navController.navigate(it.name)
                if (it == LoginStep.DONE) {
                    afterAuth(true)
                }
            }
        }
    }

    LaunchedEffect(true) {
        loginVM.oauthIntent.collect {
            if (it != null) {
                Log.d(logTag, "New non null intent received: $it")
                ctx.startActivity(it)
            }
        }
    }

    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    Column {

        // TODO nice linear progress indicator with animated steps that are bound to the current page
        // LinearProgressIndicator()

        NavHost(
            navController = navController,
            startDestination = LoginStep.URL.name,
            modifier = Modifier.weight(.8f)
        ) {
            composable(LoginStep.URL.name) {
                AskServerUrl(loginVM)
                loginVM.setCurrentStep(LoginStep.URL)
            }

            composable(LoginStep.SKIP_VERIFY.name) {
                SkipVerify(loginVM)
                loginVM.setCurrentStep(LoginStep.SKIP_VERIFY)
            }

            composable(LoginStep.P8_CRED.name) {
                P8Credentials(loginVM)
                loginVM.setCurrentStep(LoginStep.P8_CRED)
            }

            composable(LoginStep.OAUTH_FLOW.name) {
                ProcessAuth(loginVM)
                loginVM.setCurrentStep(LoginStep.OAUTH_FLOW)
            }

            composable(LoginStep.POST_AUTH.name) {
                ProcessAuth(loginVM)
                loginVM.setCurrentStep(LoginStep.POST_AUTH)
            }

            composable(LoginStep.DONE.name) {
                ProcessAuth(loginVM)
                loginVM.setCurrentStep(LoginStep.DONE)
            }
        }

        if (Str.notEmpty(message.value)) {
            Text(
                text = message.value!!,
                color = danger
            )
        }
        if (Str.notEmpty(errMsg.value)) {
            Text(
                text = errMsg.value!!,
                color = danger
            )
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
