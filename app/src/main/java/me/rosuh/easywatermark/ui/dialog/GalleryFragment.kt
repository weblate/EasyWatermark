package me.rosuh.easywatermark.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.rosuh.easywatermark.R
import me.rosuh.easywatermark.data.repo.WaterMarkRepository
import me.rosuh.easywatermark.databinding.FragmentGalleryBinding
import me.rosuh.easywatermark.ui.MainActivity
import me.rosuh.easywatermark.ui.adapter.GalleryAdapter
import me.rosuh.easywatermark.ui.base.BaseBindBSDFragment
import me.rosuh.easywatermark.utils.FileUtils
import me.rosuh.easywatermark.utils.MultiPickContract
import me.rosuh.easywatermark.utils.ktx.colorOnSurface


class GalleryFragment : BaseBindBSDFragment<FragmentGalleryBinding>() {

    private val galleryAdapter by lazy { GalleryAdapter() }

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher =
            registerForActivityResult(MultiPickContract()) { uri: List<Uri?>? ->
                handleActivityResult(uri)
            }
        shareViewModel.query(requireContext().contentResolver)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.isDraggable = false
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun bindView(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGalleryBinding {
        val rootView = FragmentGalleryBinding.inflate(layoutInflater, container, false)
        rootView.topAppBar.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
        rootView.fab.apply {
            setOnClickListener {
                shareViewModel.selectGallery(galleryAdapter.getSelectedList())
                hide()
                dismissAllowingStateLoss()
            }
        }
        rootView.rvContent.apply {
            adapter = galleryAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
            setHasFixedSize(true)
        }

//        rootView.ivSysImage.apply {
//            setOnClickListener {
//                val mime = "image/*"
//                pickImageLauncher.launch(mime)
//            }
//        }
        rootView.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.iv_sys_image -> {
                    val mime = "image/*"
                    pickImageLauncher.launch(mime)
                    return@setOnMenuItemClickListener true
                }
            }
            return@setOnMenuItemClickListener false
        }

        shareViewModel.galleryPickedImageList.observe(this) {
            galleryAdapter.submitList(it)
        }
        galleryAdapter.selectedCount.observe(this) {
            if (it > 0) {
                rootView.fab.apply {
                    text = requireContext().getString(R.string.gallery_pick_size, it)
                    show()
                }
            } else {
                rootView.fab.hide()
            }
        }
        return rootView
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        shareViewModel.resetGalleryData()
    }

    private fun handleActivityResult(list: List<Uri?>?) {
        val finalList = list?.filterNotNull()?.filter {
            FileUtils.isImage(requireContext().contentResolver, it)
        } ?: emptyList()
        if (finalList.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.tips_do_not_choose_image),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (FileUtils.isImage(requireContext().contentResolver, finalList.first())) {
            shareViewModel.updateImageList(finalList)
            dismissAllowingStateLoss()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.tips_choose_other_file_type),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}