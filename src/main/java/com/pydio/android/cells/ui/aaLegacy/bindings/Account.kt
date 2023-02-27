package com.pydio.android.cells.ui.aaLegacy.bindings

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView

@BindingAdapter("accountStatus")
fun ImageView.setAccountImage(item: RSessionView?) {
    item?.let {
        setImageResource(
            when (item.authStatus) {
                //AUTH_STATUS_NEW -> R.drawable.icon_folder
                AppNames.AUTH_STATUS_NO_CREDS -> R.drawable.ic_outline_running_with_errors_24
                AppNames.AUTH_STATUS_EXPIRED -> R.drawable.ic_outline_running_with_errors_24
                AppNames.AUTH_STATUS_UNAUTHORIZED -> R.drawable.ic_outline_running_with_errors_24
                AppNames.AUTH_STATUS_REFRESHING -> R.drawable.ic_baseline_wifi_protected_setup_24
                AppNames.AUTH_STATUS_CONNECTED -> R.drawable.ic_baseline_check_24
                else -> R.drawable.empty
            }
        )

        imageTintList = ContextCompat.getColorStateList(
            this.context,
            when (item.authStatus) {
                AppNames.AUTH_STATUS_NO_CREDS -> R.color.danger
                AppNames.AUTH_STATUS_EXPIRED -> R.color.danger
                AppNames.AUTH_STATUS_UNAUTHORIZED -> R.color.danger
                AppNames.AUTH_STATUS_REFRESHING -> R.color.warning
                AppNames.AUTH_STATUS_CONNECTED -> R.color.ok
                else -> R.color.transparent
            }
        )
    }
}

@BindingAdapter("authAction")
fun ImageView.setAuthAction(item: RSessionView?) {
    item?.let {
        setImageResource(
            when (item.authStatus) {
                //AUTH_STATUS_NEW -> R.drawable.icon_folder
                AppNames.AUTH_STATUS_NO_CREDS -> R.drawable.ic_baseline_login_24
                AppNames.AUTH_STATUS_EXPIRED -> R.drawable.ic_baseline_login_24
                AppNames.AUTH_STATUS_UNAUTHORIZED -> R.drawable.ic_baseline_login_24
                AppNames.AUTH_STATUS_REFRESHING -> R.drawable.ic_baseline_login_24
                AppNames.AUTH_STATUS_CONNECTED -> R.drawable.ic_baseline_logout_24
                else -> R.drawable.empty
            }
        )
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("account_primary_text")
fun TextView.setAccountPrimaryText(item: RSessionView?) {
    item?.let {
        text = item.serverLabel()
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("account_secondary_text")
fun TextView.setAccountSecondaryText(item: RSessionView?) {
    item?.let {
        text = "${item.username}@${item.url}"
    }
}

@BindingAdapter("session_status_desc")
fun TextView.setSessionStatusDesc(item: RSessionView?) {
    item?.let {
        val errorMsg = this.resources.getString(
            when (item.authStatus) {
                AppNames.AUTH_STATUS_EXPIRED -> R.string.auth_err_expired
                AppNames.AUTH_STATUS_CONNECTED -> R.string.auth_ok
                else -> R.string.auth_err_no_token
            }
        )

        text = this.resources.getString(
            R.string.no_connection_status,
            item.url,
            item.username,
            errorMsg
        )
        "[${item.authStatus}] $errorMsg"
    }
}

@BindingAdapter("accountHomePrimary")
fun TextView.setAccountHomePrimary(item: RSessionView?) {
    item?.let {
        // text = this.resources.getString(R.string.account_server_label, item.url)
        //text = it.username.uppercase(Locale.getDefault())
        text = it.username
    }
}

@BindingAdapter("accountHomeSecondary")
fun TextView.setAccountHomeSecondary(item: RSessionView?) {
    item?.let {
//        text = this.resources.getString(R.string.account_logged_in_as_label, item.username)
        text = it.url
    }
}

@BindingAdapter("decorateWithStateColor")
fun View.setStateColor(item: RSessionView?) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        // Do nothing for the time being
        return
    }
    item?.let {
        if (it.lifecycleState == AppNames.LIFECYCLE_STATE_FOREGROUND) {
            setBackgroundColor(resources.getColor(R.color.selected_background, context.theme))

        }
    }
}
