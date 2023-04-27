package com.pydio.android.cells.ui.login

import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.StateID

sealed class LoginDestinations(val route: String) {

    companion object {
        protected const val PREFIX = "login"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    object Starting : LoginDestinations("${PREFIX}/starting/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/starting/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/starting/") ?: false
    }

    object Done : LoginDestinations("${PREFIX}/done/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/done/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
//        override fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/done/") ?: false
    }

    object AskUrl : LoginDestinations("${PREFIX}/ask-url") {

        fun createRoute() = "${PREFIX}/ask-url"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/ask-url") ?: false
    }

    object SkipVerify : LoginDestinations("${PREFIX}/skip-verify/{${AppKeys.STATE_ID}}") {

        fun createRoute(stateID: StateID) = "${PREFIX}/skip-verify/${stateID.id}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/skip-verify/") ?: false
    }

    object P8Credentials :
        LoginDestinations("${PREFIX}/p8-credentials/{${AppKeys.STATE_ID}}/{${AppKeys.SKIP_VERIFY}}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/p8-credentials/${stateID.id}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/p8-credentials/") ?: false
    }


    object LaunchAuthProcessing :
        LoginDestinations("${PREFIX}/launch-auth-processing/{${AppKeys.STATE_ID}}/{${AppKeys.SKIP_VERIFY}}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/launch-auth-processing/${stateID.id}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/launch-auth-processing/") ?: false
    }

    object ProcessAuth :
//        LoginDestinations("${PREFIX}/process-auth/{${AppKeys.STATE_ID}}/{${AppKeys.SKIP_VERIFY}}") {
        LoginDestinations("${PREFIX}/process-auth/{${AppKeys.STATE_ID}}") {

        fun createRoute(stateID: StateID) =
            "${PREFIX}/process-auth/${stateID.id}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/process-auth/") ?: false
    }
}
