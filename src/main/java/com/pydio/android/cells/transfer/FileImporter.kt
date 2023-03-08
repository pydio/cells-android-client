package com.pydio.android.cells.transfer

//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import android.util.Log
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.ActivityResultRegistry
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.FileProvider
//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.LifecycleOwner
//import com.google.android.material.bottomsheet.BottomSheetDialogFragment
//import com.pydio.cells.transport.StateID
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.koin.core.component.KoinComponent
//import org.koin.core.component.inject
//import com.pydio.android.cells.AppNames
//import com.pydio.android.cells.services.FileService
//import com.pydio.android.cells.services.TransferService
//import com.pydio.android.cells.ui.menus.TreeNodeMenuViewModel
//import com.pydio.android.cells.utils.DEFAULT_FILE_PROVIDER_ID
//import java.io.File
//import java.io.IOException

//class FileImporter(
//    private val registry: ActivityResultRegistry,
////    private val nodeMenuVM: TreeNodeMenuViewModel,
//    private val caller: String,
//    private val callingFragment: BottomSheetDialogFragment,
//) : DefaultLifecycleObserver, KoinComponent {
//
//    private val logTag = "FileImporter"
//    private val getContentKey = AppNames.KEY_PREFIX_ + "select.files"
//    private val takePictureKey = AppNames.KEY_PREFIX_ + "take.picture"
//
//    private val fileService: FileService by inject()
//    private val transferService: TransferService by inject()
//
//    private lateinit var getMultipleContents: ActivityResultLauncher<String>
//    private lateinit var takePicture: ActivityResultLauncher<Uri>
//
//    override fun onCreate(owner: LifecycleOwner) {
//        getMultipleContents = registry.register(
//            getContentKey,
//            owner,
//            ActivityResultContracts.GetMultipleContents()
//        )
//        { uris ->
//            for (uri in uris) {
//                transferService.enqueueUpload(nodeMenuVM.stateIDs[0], uri)
//            }
//            callingFragment.dismiss()
//        }
//
//        takePicture = registry.register(takePictureKey, owner, TakePictureToInternalStorage())
//        { pictureTaken ->
//            if (!pictureTaken) {
//                callingFragment.dismiss()
//            } else {
//                nodeMenuVM.targetUri?.let {
//                    transferService.enqueueUpload(nodeMenuVM.stateIDs[0], it)
//                    callingFragment.dismiss()
//                }
//            }
//        }
//    }
//
//    fun selectFiles() { // we do not assume any specific type
//        getMultipleContents.launch("*/*")
//    }
//
//    suspend fun takePicture(stateID: StateID) = withContext(Dispatchers.IO) {
//        doTakePicture(stateID)
//    }
//
//    private fun doTakePicture(stateID: StateID) {
//        val photoFile: File? = try {
//            fileService.createImageFile(stateID)
//        } catch (ex: IOException) {
//            Log.e(logTag, "Cannot create picture file")
//            ex.printStackTrace()
//            // Error occurred while creating the File
//            null
//        }
//        // Continue only if the File was successfully created
//        photoFile?.also {
//            val uri: Uri = FileProvider.getUriForFile(
//                callingFragment.requireContext(),
//                DEFAULT_FILE_PROVIDER_ID,
//                it
//            )
//            nodeMenuVM.prepareImport(uri)
//            takePicture.launch(uri)
//        }
//    }
//}
//
//private class TakePictureToInternalStorage : ActivityResultContracts.TakePicture() {
//    override fun createIntent(context: Context, input: Uri): Intent {
//        return super.createIntent(context, input).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//    }
//}
