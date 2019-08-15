package com.example.cameraximpl

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraximpl.CameraXActivity.Companion.CAPTURED_IMAGE_ABSOLUTE_PATH
import com.example.cameraximpl.CameraXActivity.Companion.REQ_CODE_START_CAMERA_X
import com.example.cameraximpl.CameraXActivity.Companion.START_CAMERA_X
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wireEventHandlers()
    }

    private fun wireEventHandlers() {
        start_camera.setOnClickListener {
            startActivityForResult(Intent(START_CAMERA_X), REQ_CODE_START_CAMERA_X)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            requestCode == REQ_CODE_START_CAMERA_X -> {

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val absolutePath = data.getStringExtra(CAPTURED_IMAGE_ABSOLUTE_PATH)
                    captured_image_view.setImageURI(Uri.fromFile(File(absolutePath)))

                } else captured_image_view.setImageBitmap(null)
            }
        }
    }
}
