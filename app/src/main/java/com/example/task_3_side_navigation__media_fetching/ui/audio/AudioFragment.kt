package com.example.task_3_side_navigation__media_fetching.ui.audio

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.task_3_side_navigation__media_fetching.databinding.FragmentAudioBinding
import java.io.IOException
import android.Manifest
import android.app.AlertDialog
import android.media.AudioRecord
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.task_3_side_navigation__media_fetching.CommonKeys
import com.example.task_3_side_navigation__media_fetching.ProjectConstants
import com.example.task_3_side_navigation__media_fetching.R


class AudioFragment : Fragment() {
    private var isRecording = false
    private var audioUri: Uri? = null
    private lateinit var binding: FragmentAudioBinding
    private lateinit var audioActivityResultLauncher: ActivityResultLauncher<Intent>

    private val _binding get() = binding
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayerStorage: MediaPlayer? = null
    private var audioFilePath: String? = null
    private val requestStoragePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                pickAudioFromStorage()
                Log.i("Permission: ", getString(R.string.granted))
            } else {
                Log.i("Permission: ", getString(R.string.denied))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAudioBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.audioShowHide.visibility = View.GONE
        binding.stopButton.visibility = View.GONE

        audioActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    if (data != null && data.data != null) {
                        audioUri = data.data
                        audioFilePath = audioUri?.let { getRealPathFromURI(it) }
                        binding.audioShowHide.visibility = View.VISIBLE
                        binding.stopButton.visibility = View.VISIBLE
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.audio_imported),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        clickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayerStorage?.release()
        mediaPlayerStorage = null
    }


    private fun clickListeners() {
        binding.recordAudio.setOnClickListener {
            if (hasRecordAudioPermission()) {
                if (!isRecording) {
                    val recorder = startRecording()
                    if (recorder != null) {
                        startRecording()
                        isRecording = true
                        binding.recordAudio.text = getString(R.string.recording_tap_to_stop)
                        binding.stopButton.visibility = View.GONE

                    }
                } else {
                    stopRecording()
                    isRecording = false
                    binding.recordAudio.text = getString(R.string.record_audio)
                    binding.stopButton.visibility = View.VISIBLE

                }
            } else {
                requestMicPermission()
            }
        }

        binding.importAudio.setOnClickListener {
            if (hasStoragePermission()) {
                pickAudioFromStorage()
            } else {
                requestStoragePermission()
            }
        }

        binding.playAudioButton.setOnClickListener {
            if (audioFilePath != null || audioUri != null) {
                playAudioForImportedMedia()
                playAudio()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_data_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.stopButton.setOnClickListener(View.OnClickListener {
            mediaPlayer?.release()
            mediaPlayer = null
            mediaPlayerStorage?.release()
            mediaPlayerStorage = null
        })
    }

    private fun requestMicPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.RECORD_AUDIO
            )
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.mic_permission_needed))
                .setMessage(getString(R.string.requires_mic_access))
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
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                ProjectConstants.RECORD_AUDIO_PERMISSION_CODE
            )

        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.storage_permission_needed))
                .setMessage(getString(R.string.requires_storage_access_audio))
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
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ProjectConstants.STORAGE_PERMISSION_CODE
            )
        }

    }

    private fun hasRecordAudioPermission(): Boolean {
        val permission = Manifest.permission.RECORD_AUDIO
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun startRecording() {
        mediaRecorder = MediaRecorder()
        audioFilePath = "${requireContext().externalCacheDir}/audio.3gp"

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)

            try {
                prepare()
            } catch (e: IOException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.recording_failed),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            start()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        binding.audioShowHide.visibility = View.VISIBLE
        Toast.makeText(requireContext(), getString(R.string.recording_stop), Toast.LENGTH_SHORT)
            .show()
    }

    private fun pickAudioFromStorage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        audioActivityResultLauncher.launch(intent)
    }

    private fun playAudio() {
        if (audioFilePath != null) {
            mediaPlayerStorage?.release()
            mediaPlayerStorage = null

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(requireContext(), Uri.parse(audioFilePath))
                mediaPlayer?.prepare()
            }
            mediaPlayer?.start()
        }
    }

    private fun playAudioForImportedMedia() {
        if (audioUri != null) {
            mediaPlayer?.release()
            mediaPlayer = null

            if (mediaPlayerStorage == null) {
                mediaPlayerStorage = MediaPlayer()
                mediaPlayerStorage?.setDataSource(requireContext(), audioUri!!)
                mediaPlayerStorage?.prepare()
            }
            mediaPlayerStorage?.start()
        }
    }


    private fun getRealPathFromURI(uri: Uri): String? {
        val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
        return try {
            cursor?.use {
                it.moveToFirst()
                val columnIndex = it.getColumnIndexOrThrow(CommonKeys.MEDIA_PATH)
                it.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }


}