package com.pydio.android.cells.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pydio.cells.transport.ClientData
import com.pydio.android.cells.BuildConfig
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAboutBinding
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.utils.getOSCurrentVersion
import com.pydio.android.cells.utils.getTimestampAsENString
import com.pydio.android.cells.utils.getTimestampAsString
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AboutFragment : Fragment() {

    private val logTag = AboutFragment::class.simpleName

    // We inject this to trigger state saving so that we stay on this page upon screen rotation.
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        val data = ClientData.getInstance()
        setHasOptionsMenu(true)
        val binding: FragmentAboutBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_about, container, false
        )

        binding.aboutVersionName.text =
            resources.getString(R.string.version_name_display, data.version)
        binding.aboutVersionCode.text =
            resources.getString(R.string.version_code_display, BuildConfig.VERSION_CODE.toString())

        val dateString = getTimestampAsString(data.buildTimestamp)
        binding.aboutVersionDate.text =
            resources.getString(R.string.version_date_display, dateString)

        binding.mainWebsiteButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(resources.getString(R.string.main_website))
            startActivity(intent)
        }

        binding.sendSupportMailButton.setOnClickListener {
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

        return binding.root
    }
}
