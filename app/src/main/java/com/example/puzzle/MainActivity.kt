package com.example.puzzle


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    var mCurrentPhotoPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val am = assets
        try {
            val files = am.list("img")
            val grid = findViewById<GridView>(R.id.grid)
            grid.adapter = ImageAdapter(this)
            grid.onItemClickListener =
                OnItemClickListener { adapterView, view, i, l ->
                    val intent = Intent(applicationContext, PuzzleActivity::class.java)
                    intent.putExtra("assetName", files!![i % files.size])
                    startActivity(intent)
                }
        } catch (e: IOException) {
            Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_SHORT)
        }
    }

    fun onImageFromCameraClick(view: View?) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG)
            }
            if (photoFile != null) {
                val photoUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    applicationContext.packageName + ".fileprovider",
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission not granted, initiate request
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
            )
        } else {
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
            )
            mCurrentPhotoPath = image.absolutePath // save this to use in the intent
            return image
        }
        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onImageFromCameraClick(View(this@MainActivity))
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val intent = Intent(this@MainActivity, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoPath", mCurrentPhotoPath)
            startActivity(intent)
        }
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK) {
            val uri = data!!.data
            val intent = Intent(this@MainActivity, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoUri", uri.toString())
            startActivity(intent)
        }
    }

    fun onImageFromGalleryClick(view: View?) {
         object {

            fun chooseFromGallery(activity: Activity, galleryLauncher: ActivityResultLauncher<Intent>) {
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

                if (ContextCompat.checkSelfPermission(
                        activity.applicationContext,
                        permissions[0]
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity,
                        permissions,
                        RequestCode.REQUEST_CODE_IMAGE_GALLERY.ordinal
                    )
                } else {
                    chooseFromGalleryIntent(galleryLauncher)
                }
            }

            /**
             * Launches the Gallery should the user decide to choose an image from it.
             *
             * @param galleryLauncher Activity result launcher related to choosing an image from the Gallery.
             */
            private fun chooseFromGalleryIntent(galleryLauncher: ActivityResultLauncher<Intent>) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"

                galleryLauncher.launch(intent)
            }

            /**
             * Defines the behavior depending on whether the user granted (or denied) the necessary
             * permission for choosing an image from the Gallery.
             *
             * @param grantResults Grant results for the corresponding permissions which is either <code>
             *     PackageManager.PERMISSION_GRANTED</code> or <code>PackageManager.PERMISSION_DENIED</code>.
             *     Never null.
             * @param context Context tied to the activity calling this method.
             * @param galleryLauncher Activity result launcher related to choosing an image from the Gallery.
             */
            fun permissionsResultGallery(
                grantResults: IntArray, context: Context,
                galleryLauncher: ActivityResultLauncher<Intent>
            ) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseFromGalleryIntent(galleryLauncher)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
        private const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3
        const val REQUEST_IMAGE_GALLERY = 4
    }
}



