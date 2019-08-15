package com.example.cameraximpl


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File

class CameraXActivity : AppCompatActivity() {

    // This is an arbitrary number we are using to keep tab of the permission
    // request. Where an app has multiple context for requesting permission,
    // this can help differentiate the different contexts
    private val REQUEST_CODE_PERMISSIONS = 10

    // This is an array of all the permission specified in the manifest
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var lensFacing = CameraX.LensFacing.BACK

    private var imageFile: File? = null

    private var isCameraXBindedToActivityLifecycle = false

    companion object {
        val CAPTURED_IMAGE_ABSOLUTE_PATH = "CAPTURED_IMAGE_ABSOLUTE_PATH"
        val START_CAMERA_X = "START_CAMERA_X"
        val REQ_CODE_START_CAMERA_X = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        wireEventHandlers()
        captureImage()
    }

    override fun onPause() {
        super.onPause()
        isCameraXBindedToActivityLifecycle = false
    }


    private fun wireEventHandlers() {
        re_capture_button.setOnClickListener {
            showCameraPreview()
        }
    }

    private fun captureImage() {
        checkPermission()
    }

    private fun deleteImageFile() {
        imageFile?.delete()
        imageFile = null
    }

    private fun startCamera() {
        // If the user is trying to re-capture the image, then old image need to be deleted
        deleteImageFile()

        texture_view.visibility = View.VISIBLE

        texture_view.surfaceTexture?.release()

        val metrics = DisplayMetrics().also { texture_view.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(windowManager.defaultDisplay.rotation)
            setTargetRotation(texture_view.display.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            texture_view.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setLensFacing(lensFacing)
                setTargetAspectRatio(screenAspectRatio)
                setTargetRotation(texture_view.display.rotation)
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)

        click_button.setOnClickListener {
            click_button.setOnClickListener(null)

            imageFile = File(
                Environment.getExternalStorageDirectory().toString()
                        + "/"
                        + "${System.currentTimeMillis()}.jpg"
            )

            imageCapture.takePicture(imageFile,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        error: ImageCapture.UseCaseError,
                        message: String, exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }

                    override fun onImageSaved(file: File) {
                        finishSuccessfully()

//                             TODO - currently the re-capture feature is not working, so disabling the image preview
//                            showCapturedImage()
                    }
                })
        }

        if (!isCameraXBindedToActivityLifecycle) {
            CameraX.bindToLifecycle(this, preview, imageCapture)
            isCameraXBindedToActivityLifecycle = true;
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = texture_view.width / 2f
        val centerY = texture_view.height / 2f

        val rotationDegrees = when (texture_view.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        texture_view.setTransform(matrix)
    }

    private fun checkPermission() {
        if (allPermissionsGranted()) {
            texture_view.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        texture_view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                texture_view.post {
                    startCamera()
                }

            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()

                finishUnsuccessfully()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*This will show the image that has been captured*/
    private fun showCapturedImage() {
        image_view.setImageBitmap(BitmapFactory.decodeFile(imageFile.toString()))

        texture_view.visibility = View.GONE
        click_button.visibility = View.GONE

        image_view.visibility = View.VISIBLE
        ok_button.visibility = View.VISIBLE
        re_capture_button.visibility = View.VISIBLE

        initOkButtonHandler()
    }

    private fun initOkButtonHandler() {
        ok_button.setOnClickListener {
            finishSuccessfully()
        }
    }

    /*Hide the image captured and restart the camera preview again*/
    private fun showCameraPreview() {
        texture_view.visibility = View.VISIBLE
        click_button.visibility = View.VISIBLE

        image_view.visibility = View.GONE
        ok_button.visibility = View.GONE
        re_capture_button.visibility = View.GONE

        startCamera()
    }

    private fun finishSuccessfully() {
        destroyTextureviewSurface()
        setResultOKAndAddIntentData()
        finishAndRemoveTask()
    }

    private fun finishUnsuccessfully() {
        destroyTextureviewSurface()
        setResult(Activity.RESULT_CANCELED)
        finishAndRemoveTask()
    }

    private fun setResultOKAndAddIntentData() {
        val absolutePath = imageFile?.absolutePath

        val intent = Intent()
        intent.putExtra(CAPTURED_IMAGE_ABSOLUTE_PATH, absolutePath)

        setResult(Activity.RESULT_OK, intent)
    }

    private fun destroyTextureviewSurface() {
        texture_view.surfaceTexture?.release()

        try {
            texture_view.surfaceTexture.detachFromGLContext()
        } catch (ignore: Exception) {
        }

        CameraX.unbindAll()
    }
}