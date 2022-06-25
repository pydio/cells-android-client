package com.pydio.android.cells.ui.menus

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.MoreMenuPrefBinding
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.ui.bindings.convertDpToPixel
import org.koin.android.ext.android.inject

/**
 * Simple bottom menu to manage a given preference with a list.
 */
class PrefListMenuFragment : BottomSheetDialogFragment() {

    // dirty tweak
    val noValue = "<null>"

    private val logTag = PrefListMenuFragment::class.java.simpleName

    private val prefs: CellsPreferences by inject()
    private lateinit var oldPref: String

    private lateinit var prefBinding: MoreMenuPrefBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefBinding = DataBindingUtil.inflate(
            inflater, R.layout.more_menu_pref, container, false
        )

        val parentLayout = prefBinding.moreMenuPref

        val args: PrefListMenuFragmentArgs by navArgs()
        val prefKey = args.preferenceKey
        val valueListKey = args.wellKnownValuesKey
        oldPref = prefs.getString(prefKey, args.defValue)

        Log.d(logTag, "onCreateView, default filter: $oldPref")

        var titleId = getResourceIdByName(requireContext(), "string", "${prefKey}_title")
        if (titleId == 0) {
            titleId = getResourceIdByName(requireContext(), "string", "${valueListKey}_title")
        }

        val currPrefKnownValuesId =
            getResourceIdByName(requireContext(), "array", "${valueListKey}_values")

        if (titleId == 0 || currPrefKnownValuesId == 0) {
            Log.e(
                logTag, "could not find resources for $prefKey, aborting... " +
                        "(titleId: $titleId, knownValuesId: $currPrefKnownValuesId)"
            )
            dismiss()
            return null
        }
        prefBinding.moreMenuPrefTitle.text = resources.getString(titleId)

        val keys = resources.getStringArray(currPrefKnownValuesId)
        for (currKey in keys) {

            val rowLayout = LinearLayout(requireContext())
            parentLayout.addView(rowLayout)
            rowLayout.orientation = LinearLayout.VERTICAL
            rowLayout.addBackgroundRipple()

            rowLayout.setOnClickListener {
                // showMessage(requireContext(), currKey)
                prefs.setString(prefKey, currKey)
                dismiss()
            }
            if (currKey == oldPref) {
                // TODO this does not work yet
                rowLayout.isSelected = true
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val style =
                if (currKey == oldPref) R.style.TextAppearance_Material3_TitleMedium else R.style.TextAppearance_Material3_BodyLarge
            val textView =
                TextView(requireContext(), null, 0, style)

            val labelId =
                getResourceIdByName(requireContext(), "string", "${valueListKey}_${currKey}_label")
            textView.text = if (labelId > 0) {
                resources.getString(labelId)
            } else {
                currKey
            }
            textView.layoutParams = params
            val hPadding =
                convertDpToPixel(requireContext(), resources.getDimension(R.dimen.margin_small))
            val vPadding =
                convertDpToPixel(requireContext(), resources.getDimension(R.dimen.margin_xsmall))
            textView.setPadding(hPadding, vPadding, hPadding, vPadding)

            rowLayout.addView(textView)
        }

        return prefBinding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(logTag, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(logTag, "onStop")
    }
}

// TODO this uses reflection and must be handled carefully when re-enabling pro-guard
private fun getResourceIdByName(ctx: Context, defType: String, stringId: String?): Int {
    return ctx.resources.getIdentifier(stringId, defType, ctx.packageName)
}

// see https://stackoverflow.com/questions/37987732/programmatically-set-selectableitembackground-on-android-view
fun View.addBackgroundRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}