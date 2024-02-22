package com.example.task_3_side_navigation__media_fetching.ui.image

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.task_3_side_navigation__media_fetching.CommonKeys
import com.example.task_3_side_navigation__media_fetching.ProjectConstants
import com.example.task_3_side_navigation__media_fetching.R
import com.example.task_3_side_navigation__media_fetching.databinding.FragmentAudioBinding
import com.example.task_3_side_navigation__media_fetching.databinding.FragmentImageBinding
import com.google.android.material.snackbar.Snackbar

class ImageFragment : Fragment() {
    private lateinit var viewModel: ImageViewModel
    private lateinit var binding: FragmentImageBinding
    private val _binding get() = binding
    private lateinit var imageActivityResultLauncher: ActivityResultLauncher<Intent>
    private val requestPermissionLauncher =
        registerForActivityResult(
            RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
                Log.i("Permission: ", getString(R.string.granted))
            } else {
                Log.i("Permission: ", getString(R.string.denied))
            }
        }
    private val requestStoragePermissionLauncher =
            registerForActivityResult(
                RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    openGallery()
                    Log.i("Permission: ", getString(R.string.granted))
                } else {
                    Log.i("Permission: ", getString(R.string.denied))
                }
            }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageBinding.inflate(inflater, container, false)
        return _binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ImageViewModel::class.java]

        viewModel.getCaptureImage()?.let { imageBitmap ->
            binding.imageView.setImageBitmap(imageBitmap)
        }
        viewModel.getGalleryImage()?.let { galleryImageBitmap ->
            binding.imageView.setImageBitmap(galleryImageBitmap)
        }

        imageActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    val imageBitmap: Bitmap? = data?.extras?.get(CommonKeys.IMAGE_BITMAP_KEY) as Bitmap?
                    if (imageBitmap != null) {
                        val image = data?.extras?.get(CommonKeys.IMAGE_BITMAP_KEY) as Bitmap
                        viewModel.setCaptureImage(image)
                        binding.imageView.setImageBitmap(image)

                    }

                    val imageUri = data?.data
                    if (imageUri != null) {
                        val loadedImage = loadBitmapFromUri(imageUri)
                        viewModel.setGalleryImage(loadedImage)
                        binding.imageView.setImageBitmap(loadedImage)
                    }
                }
            }

        clickListeners()

    }

    private fun clickListeners() {

        binding.captureImage.setOnClickListener(View.OnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        })

        binding.importImageFromGallery.setOnClickListener(View.OnClickListener {
            if (hasStoragePermission()) {

                openGallery()
            } else {

                requestStoragePermission()
            }
        })

    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.CAMERA))
        {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.camera_permission_needed))
                .setMessage(getString(R.string.requires_camera_access))
                .setPositiveButton(getString(R.string.settings)) { _, _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(CommonKeys.PACKAGE, requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()

        } else {
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                ProjectConstants.CAMERA_PERMISSION_CODE
            )

        }

    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.storage_permission_needed))
                .setMessage(getString(R.string.requires_storage_access))
                .setPositiveButton(getString(R.string.settings)) { _, _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(CommonKeys.PACKAGE, requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else {
            requestStoragePermissionLauncher.launch(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ProjectConstants.STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imageActivityResultLauncher.launch(galleryIntent)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageActivityResultLauncher.launch(cameraIntent)
    }
    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

}