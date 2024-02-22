package com.example.task_3_side_navigation__media_fetching.ui.video

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.task_3_side_navigation__media_fetching.CommonKeys
import com.example.task_3_side_navigation__media_fetching.ProjectConstants
import com.example.task_3_side_navigation__media_fetching.R
import com.example.task_3_side_navigation__media_fetching.databinding.FragmentVideoBinding
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory

class VideoFragment : Fragment() {
    private var recordedVideoUri: Uri? = null
    private var exoPlayer: ExoPlayer? = null
    private var isPlaying = false
    private var isFragmentResumed = false
    private var isPlayButtonClicked = false
    private lateinit var videoActivityResultLauncher: ActivityResultLauncher<Intent>
    private val requestCameraVideoPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                recordVideo()
                Log.i("Permission: ", getString(R.string.granted))
            } else {
                Log.i("Permission: ", getString(R.string.denied))
            }
        }
    private val requestStoragePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                importVideo()
                Log.i("Permission: ", getString(R.string.granted))
            } else {
                Log.i("Permission: ", getString(R.string.denied))
            }
        }

    private lateinit var binding: FragmentVideoBinding
    private val _binding get() = binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    if (data != null) {
                        val selectedVideoUri = data.data
                        if (selectedVideoUri != null) {
                            recordedVideoUri = selectedVideoUri
                            binding.videoThumbnail.setImageBitmap(
                                createVideoThumbnail(
                                    recordedVideoUri!!
                                )
                            )
                            binding.playButton.setImageResource(R.drawable.play)
                            binding.playButton.visibility = View.VISIBLE
                            isPlaying = false
                        }
                    }
                    val capturedVideoUri = data?.data
                    if (capturedVideoUri != null) {
                        recordedVideoUri = capturedVideoUri
                        binding.videoThumbnail.setImageBitmap(createVideoThumbnail(capturedVideoUri))
                        binding.playButton.setImageResource(R.drawable.play)
                        binding.playButton.visibility = View.VISIBLE
                        isPlaying = false
                    }

                }
            }



        clickListeners()
        binding.playButton.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        stopPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayer()
        isFragmentResumed = false
    }

    override fun onResume() {
        super.onResume()
        if (isFragmentResumed) {
            if (recordedVideoUri != null) {
                initializePlayerForRecordedVideo(recordedVideoUri!!)
                initializePlayerForImportedVideo(recordedVideoUri!!)
            }
        }
        isFragmentResumed = true
    }

    private fun clickListeners() {
        binding.importVideoFromGallery.setOnClickListener {
            if (hasStoragePermission()) {
                importVideo()
            } else {
                requestStoragePermission()
            }
        }

        binding.captureVideo.setOnClickListener {
            if (hasVideoPermission()) {
                recordVideo()
            } else {
                requestVideoPermission()
            }
        }

        binding.playButton.setOnClickListener {
            if (!isPlaying) {
                stopPlayer()
                initializePlayerForRecordedVideo(recordedVideoUri!!)
                initializePlayerForImportedVideo(recordedVideoUri!!)
            } else {
                stopPlayer()
            }
        }
    }

    private fun importVideo() {
        stopPlayer()
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        videoActivityResultLauncher.launch(intent)
    }

    private fun recordVideo() {
        stopPlayer()
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoActivityResultLauncher.launch(intent)
    }

    private fun initializePlayerForImportedVideo(uri: Uri) {
        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(DefaultMediaSourceFactory(requireContext()))
                .setLoadControl(DefaultLoadControl())
                .build()
        }

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = false

        val videoView = binding.videoView
        val videoThumbnail = binding.videoThumbnail
        val playButton = binding.playButton

        videoView.player = exoPlayer
        videoView.visibility = View.GONE
        videoThumbnail.visibility = View.VISIBLE

        playButton.visibility = View.VISIBLE
        playButton.setImageResource(R.drawable.play)

        playButton.setOnClickListener {
            videoView.visibility = View.VISIBLE
            videoThumbnail.visibility = View.GONE
            playButton.visibility = View.GONE

            isPlayButtonClicked = true
            if (exoPlayer != null) {
                exoPlayer?.playWhenReady = true
            }
        }
    }

    private fun initializePlayerForRecordedVideo(uri: Uri) {
        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(DefaultMediaSourceFactory(requireContext()))
                .setLoadControl(DefaultLoadControl())
                .build()
        }

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = false

        val videoView = binding.videoView
        val videoThumbnail = binding.videoThumbnail
        val playButton = binding.playButton

        videoView.player = exoPlayer
        videoView.visibility = View.GONE
        videoThumbnail.visibility = View.VISIBLE

        playButton.visibility = View.VISIBLE
        playButton.setImageResource(R.drawable.play)

        playButton.setOnClickListener {
            videoView.visibility = View.VISIBLE
            videoThumbnail.visibility = View.GONE
            playButton.visibility = View.GONE

            isPlayButtonClicked = true
            if (exoPlayer != null) {
                exoPlayer?.playWhenReady = true
            }
        }
    }

    private fun stopPlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null

        binding.videoView.visibility = View.GONE
        binding.videoThumbnail.visibility = View.VISIBLE
        binding.playButton.setImageResource(R.drawable.play)
        isPlayButtonClicked = false
    }

    private fun createVideoThumbnail(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(requireContext(), videoUri)
            retriever.frameAtTime
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun hasVideoPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestVideoPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.CAMERA
            )
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.camera_permission_needed))
                .setMessage(getString(R.string.requires_camera_access_video))
                .setPositiveButton(getString(R.string.settings)) { _, _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(CommonKeys.PACKAGE, requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            requestCameraVideoPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                ProjectConstants.CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.storage_permission_needed))
                .setMessage(getString(R.string.requires_storage_access_video))
                .setPositiveButton(getString(R.string.settings)) { _, _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(CommonKeys.PACKAGE, requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            requestStoragePermissionLauncher.launch(
                Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ProjectConstants.STORAGE_PERMISSION_CODE
            )
        }
    }



}