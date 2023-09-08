package com.pydio.android.cells.ui.login

import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.cells.transport.StateID

sealed class LoginDestinations(val route: String) {

    companion object {
        protected const val PREFIX = "login"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    data object Starting : LoginDestinations("${PREFIX}/starting/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/starting/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/starting/") ?: false
    }

    data object Done : LoginDestinations("${PREFIX}/done/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/done/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/done/") ?: false
    }

    data object AskUrl : LoginDestinations("${PREFIX}/ask-url") {

        fun createRoute() = "${PREFIX}/ask-url"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/ask-url") ?: false
    }

    data object SkipVerify : LoginDestinations("${PREFIX}/skip-verify/{${AppKeys.STATE_ID}}") {

        fun createRoute(stateID: StateID) = "${PREFIX}/skip-verify/${encodeStateForRoute(stateID)}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/skip-verify/") ?: false
    }

    data object P8Credentials :
        LoginDestinations("${PREFIX}/p8-credentials/{${AppKeys.STATE_ID}}/{${AppKeys.SKIP_VERIFY}}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/p8-credentials/${encodeStateForRoute(stateID)}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/p8-credentials/") ?: false
    }


    data object LaunchAuthProcessing :
        LoginDestinations("${PREFIX}/launch-auth-processing/{${AppKeys.STATE_ID}}/{${AppKeys.SKIP_VERIFY}}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/launch-auth-processing/${encodeStateForRoute(stateID)}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/launch-auth-processing/") ?: false
    }

    data object ProcessAuthCallback :
        LoginDestinations("${PREFIX}/process-auth-callback/{${AppKeys.STATE_ID}}") {

        fun createRoute(stateID: StateID) =
            "${PREFIX}/process-auth-callback/${encodeStateForRoute(stateID)}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/process-auth-callback/") ?: false
    }
}
