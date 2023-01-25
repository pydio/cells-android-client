package com.pydio.android.cells.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.system.AboutScreen
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.getOSCurrentVersion
import com.pydio.android.cells.utils.getTimestampAsENString
import com.pydio.cells.transport.ClientData

class AboutFragment : Fragment() {

    private val logTag = AboutFragment::class.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        val onUriClick = {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(resources.getString(R.string.main_website))
            startActivity(intent)
        }

        val onEmailClick: () -> Unit = {
            val data = ClientData.getInstance()
            val format = resources.getString(R.string.app_info)
            val appInfo = String.format(
                format,
                data.versionCode,
                data.version,
                getTimestampAsENString(data.buildTimestamp),
                getOSCurrentVersion(),
            )
            val summary = "\n\nPlease describe your problem (in English): \n"

            val intent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
                .putExtra(Intent.EXTRA_TEXT, appInfo + summary)
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.support_email)))
            if (activity?.packageManager?.resolveActivity(intent, 0) != null) {
                startActivity(intent)
            } else {
                Log.e(logTag, "Could not trigger email for: $appInfo")
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                CellsTheme {
                    AboutScreen(onUriClick = onUriClick, onEmailClick = onEmailClick)
                }
            }
        }
    }
}
