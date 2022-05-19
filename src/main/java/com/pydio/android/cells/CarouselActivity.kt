package com.pydio.android.cells

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Carousel
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.pydio.android.cells.databinding.ActivityCarouselBinding
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.viewer.CarouselViewModel
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Basic carousel to open the supported files (for the time being only images)
 * with a nice look and feel without leaving the main app.
 */
class CarouselActivity : AppCompatActivity() {

    private val logTag = CarouselActivity::class.simpleName
    private val accountService: AccountService by inject()

    private val carouselVM: CarouselViewModel by viewModel()
// FIXME PASS at least the account ID to the view model

    private lateinit var binding: ActivityCarouselBinding

    var numImages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate, intent: $intent")
        super.onCreate(savedInstanceState)
        handleIntent(savedInstanceState)
        setupActivity()
    }

    override fun onResume() {
        Log.d(logTag, "onResume, intent: $intent")
        super.onResume()
        setupCarousel()
        lifecycleScope.launch {
            // Workaround the NPE on creation
            delay(800)
            observe()
        }
    }

    private fun handleIntent(savedInstanceState: Bundle?) {

        var state: StateID? = null
        if (savedInstanceState != null) {
            val stateStr = savedInstanceState.getString(AppNames.EXTRA_STATE)
            // TODO switch query depending on the context (browse, bookmark, offline...)
            // val contextType = intent.getStringExtra(AppNames.EXTRA_ACTION_CONTEXT)
            state = StateID.fromId(stateStr)
        } else if (intent.hasExtra(AppNames.EXTRA_STATE)) {
            val stateStr: String = intent.getStringExtra(AppNames.EXTRA_STATE)!!
            // TODO switch query depending on the context (browse, bookmark, offline...)
            // val contextType = intent.getStringExtra(AppNames.EXTRA_ACTION_CONTEXT)
            state = StateID.fromId(stateStr)
        }

        state?.let {
            carouselVM.afterCreate(state.parent(), state)
            carouselVM.allChildren.observe(this) {
                carouselVM.updateElements(it)
            }
        }

        // TODO handle errors
    }

    private fun setupActivity() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_carousel)

        // TODO if no better solution is found, open here a simple viewer with glide
        //   instead of the carousel => we have glitches when we have only one picture

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())
            binding.root.windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }

    private fun setupCarousel() {
        binding.carousel.setAdapter(MyAdapter())
    }

    private inner class MyAdapter : Carousel.Adapter {

        override fun count(): Int {
            return numImages
        }

        override fun populate(view: View, index: Int) {
            if (view !is ImageView) {
                return
            }

            // Log.d(logTag, "Populating #$index")
            val currItem = carouselVM.elements.value!![index]

            // Adding thumb for quick loading
            val thumbnailRequest: RequestBuilder<Drawable> = Glide.with(this@CarouselActivity)
                .load(encodeModel(currItem, AppNames.LOCAL_FILE_TYPE_THUMB))

            // TODO handle here
            // - using previews rather than full files when remote is cells (and when we are on a metered network? )
            // - offline / metered strategy

//            if (networkService.isNetworkMetered()) {
//                downloadThumbs = CellsApp.instance.sharedPreferences.getBoolean(
//                    AppNames.PREF_KEY_METERED_DL_THUMBS,
//                    false
//                )
//                downloadFiles = CellsApp.instance.sharedPreferences.getBoolean(
//                    AppNames.PREF_KEY_METERED_DL_FILES,
//                    false
//                )
//
//                Log.d(logTag, "Metered network, DL thumbs: $downloadThumbs, DL files: $downloadFiles")
//            }

            if (carouselVM.isRemoteLegacy) {
                Glide.with(this@CarouselActivity)
                    .load(encodeModel(currItem, AppNames.LOCAL_FILE_TYPE_FILE))
                    .thumbnail(thumbnailRequest)
                    .into(view)
            } else {
                Glide.with(this@CarouselActivity)
                    .load(encodeModel(currItem, AppNames.LOCAL_FILE_TYPE_PREVIEW))
                    .thumbnail(thumbnailRequest)
                    .into(view)
            }
        }

        override fun onNewItem(index: Int) {
            // Log.d(logTag, "on new Item #$index")
            // Retrieve the encoded state of the current item and store it in the view model
            // to stay at the same index upon restart / configuration change.
            carouselVM.elements.value?.let {
                val currItem = it[index]
                carouselVM.setActive(currItem.getStateID())
                //Log.e(logTag, "set active: ${currItem.getStateID()}")
            }
        }
    }

    private fun observe() {
        carouselVM.elements.observe(this) {
            numImages = it.size
            jumpToIndex()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        Log.i(logTag, "onPostCreate")
        super.onPostCreate(savedInstanceState, persistentState)

        jumpToIndex()
    }

    private fun jumpToIndex() {

        val currItems = carouselVM.elements.value ?: run {
            Log.e(logTag, "Could not jump to index, carousel has no elements")
            return
        }

// Does not work
//        // We disable the carousel when we have only one image
//        if (currItems.size < 2) {
//            binding.motionLayout.isEnabled = false
//        }

        var index: Int = -1
        var i = 0
        for (currNode in currItems) {
            if (currNode.encodedState == carouselVM.currActive.id) {
                index = i
                break
            }
            i++
        }

        val msg = "... Open for ${StateID.fromId(carouselVM.currActive.id)} at index: $index"
        Log.d(logTag, msg)

        if (index > 0) {
            binding.carousel.jumpToIndex(index)
        } else {
            binding.carousel.refresh()
        }
    }

    override fun onStart() {
        Log.d(logTag, "onStart, intent: $intent")
        super.onStart()
    }

    override fun onPause() {
        Log.d(logTag, "onPause, intent: $intent")
        super.onPause()
    }

    override fun onStop() {
        Log.d(logTag, "onStop, intent: $intent")
        super.onStop()
    }
}

/**
 * Avoid NPE on screen rotation and other configuration changes
 * Thanks to https://stackoverflow.com/questions/65048418/how-to-restore-transition-state-of-motionlayout-without-auto-playing-the-transit
 */
open class SavingMotionLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {

    private val tag = SavingMotionLayout::class.simpleName

    override fun onSaveInstanceState(): Parcelable {
        Log.d(tag, "onSaveInstanceState()")
        return SaveState(super.onSaveInstanceState(), startState, endState, targetPosition)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d(tag, "onRestoreInstanceState()")
        (state as? SaveState)?.let {
            super.onRestoreInstanceState(it.superParcel)
            setTransition(it.startState, it.endState)
            progress = it.progress
        }
    }

    @kotlinx.parcelize.Parcelize
    private class SaveState(
        val superParcel: Parcelable?,
        val startState: Int,
        val endState: Int,
        val progress: Float
    ) : Parcelable
}
