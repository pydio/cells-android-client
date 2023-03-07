package com.pydio.android.cells.ui.login

import com.pydio.cells.transport.StateID

sealed class LoginDestinations(val route: String) {

    companion object {
        protected const val STATE_ID_KEY = "state-id"
        protected const val SKIP_KEY = "skip-verify"
        protected const val PREFIX = "login"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    fun getStateIdKey(): String = STATE_ID_KEY
    fun getSkipVerifyKey(): String = SKIP_KEY

    object Starting : LoginDestinations("${PREFIX}/starting/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/starting/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/starting/") ?: false
    }

    object Done : LoginDestinations("${PREFIX}/done/{$STATE_ID_KEY}") {
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

    object SkipVerify : LoginDestinations("${PREFIX}/skip-verify/{$STATE_ID_KEY}") {

        fun createRoute(stateID: StateID) = "${PREFIX}/skip-verify/${stateID.id}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/skip-verify/") ?: false
    }

    object P8Credentials :
        LoginDestinations("${PREFIX}/p8-credentials/{$STATE_ID_KEY}/{$SKIP_KEY}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/p8-credentials/${stateID.id}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/p8-credentials/") ?: false
    }

    object ProcessAuth : LoginDestinations("${PREFIX}/process-auth/{$STATE_ID_KEY}/{$SKIP_KEY}") {

        fun createRoute(stateID: StateID, skipVerify: Boolean) =
            "${PREFIX}/process-auth/${stateID.id}/$skipVerify"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/process-auth/") ?: false
    }
}
