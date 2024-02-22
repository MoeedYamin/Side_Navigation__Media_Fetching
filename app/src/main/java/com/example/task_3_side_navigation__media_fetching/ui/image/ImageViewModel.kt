package com.example.task_3_side_navigation__media_fetching.ui.image

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ImageViewModel : ViewModel() {
    private var imageCapture: Bitmap? = null
    private var imageGallery: Bitmap? = null

    fun setCaptureImage(image: Bitmap) {
        imageCapture = image
    }

    fun getCaptureImage(): Bitmap? {
        return imageCapture
    }

    fun setGalleryImage(image: Bitmap) {
        imageGallery = image
    }

    fun getGalleryImage(): Bitmap? {
        return imageGallery
    }

}