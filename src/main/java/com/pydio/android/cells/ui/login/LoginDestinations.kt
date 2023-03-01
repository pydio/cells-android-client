package com.pydio.android.cells.ui.login

import com.pydio.cells.transport.StateID

sealed class LoginDestinations(val route: String) {

    companion object {
        protected const val STATE_ID_KEY = "state-id"
        protected const val UID_KEY = "uid"
        protected const val PREFIX = "login"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    fun getStateIdKey(): String = STATE_ID_KEY
    fun getUidKey(): String = UID_KEY
//    open fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}") ?: false

    object Starting : LoginDestinations("${PREFIX}/starting/{state-id}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/starting/${stateID.id}"

        //        override fun isCurrent(route: String?): Boolean =
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/starting/") ?: false
//        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    object Done : LoginDestinations("${PREFIX}/done/{state-id}") {
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

    object SkipVerify : LoginDestinations("${PREFIX}/skip-verify/{state-id}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/skip-verify/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/skip-verify/") ?: false
    }

    object P8Credentials : LoginDestinations("${PREFIX}/p8-credentials/{state-id}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/p8-credentials/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/p8-credentials/") ?: false
    }

    object ProcessAuth : LoginDestinations("${PREFIX}/process-auth/{state-id}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/process-auth/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/process-auth/") ?: false
    }
}
