package com.example.imagegallerysaver

import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageGallerySaverPlugin(private val registrar: Registrar) : MethodCallHandler {

    companion object {

        val TAG = ImageGallerySaverPlugin::class.java.simpleName

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "image_gallery_saver")
            channel.setMethodCallHandler(ImageGallerySaverPlugin(registrar))
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        when {
            call.method == "saveImageToGallery" -> {
                val image = call.arguments as ByteArray
                result.success(saveImageToGallery(BitmapFactory.decodeByteArray(image, 0, image.size)))
            }
            call.method == "saveFileToGallery" -> {
                val path = call.arguments as String
                result.success(saveFileToGallery(path))
            }
            else -> result.notImplemented()
        }

    }

    private fun debugMsg(msg: String) {
        Log.d(TAG,"debugMsg: $msg")
        Toast.makeText(registrar.activity(),msg,Toast.LENGTH_LONG).show()
    }

    private fun generateFile(extension: String = ""): File {

        val ctx = registrar.activeContext().applicationContext

        var storePath = ""
        storePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + File.separator + getApplicationName()
        } else {
            Environment.getExternalStorageDirectory().absolutePath + File.separator + getApplicationName()
        }

        debugMsg("storePath：$storePath")

        val appDir = File(storePath)
        if (!appDir.exists()) {
            var created = appDir.mkdir()
            debugMsg("storePath created:$created")
        }else{
            debugMsg("storePath exists")
        }
        var fileName = System.currentTimeMillis().toString()
        if (extension.isNotEmpty()) {
            fileName += (".$extension")
        }
        return File(appDir, fileName)
    }

    private fun saveImageToGallery(bmp: Bitmap): String {
        val file = generateFile("png")
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 60, fos)
            fos.flush()
            fos.close()
            val uri = Uri.fromFile(file)
            setVisibleInGallery(file, uri)
            return uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            debugMsg(exceptionMsg(e))
        }
        return ""
    }
    
    private fun androidQSaveImage2Gallery(bmp: Bitmap){
        val ctx = registrar.activeContext().applicationContext
        val resolver = ctx.contentResolver
        MediaStore.Images.Media.insertImage(resolver,bmp,"${System.currentTimeMillis()}.png","")
        
    }
    
    private fun exceptionMsg(e:Exception):String{
        val msg = e.message
        //val stackTrace = e.stackTrace


        return "Exception: $msg ${e.localizedMessage}"
    }

    private fun setVisibleInGallery(file: File, uri: Uri) {
        Log.d(TAG, "setVisibleInGallery")
        val ctx = registrar.activeContext().applicationContext
        Log.d(TAG, "SDK version: ${Build.VERSION.SDK_INT}, Q:${Build.VERSION_CODES.Q}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.CONTENT_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DESCRIPTION, file.name)
                put(MediaStore.Images.Media.RELATIVE_PATH, getApplicationName() + File.separator + file.name)
            }
            val result = ctx.contentResolver.insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values)
            Log.d(TAG, "result: $result")
        } else {
            ctx.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        }

    }

    private fun saveFileToGallery(filePath: String): String {
        val context = registrar.activeContext().applicationContext
        return try {
            val originalFile = File(filePath)
            val file = generateFile(originalFile.extension)
            originalFile.copyTo(file)

            val uri = Uri.fromFile(file)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            return uri.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    private fun getApplicationName(): String {
        val context = registrar.activeContext().applicationContext
        var ai: ApplicationInfo? = null
        try {
            ai = context.packageManager.getApplicationInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        var appName: String
        appName = if (ai != null) {
            val charSequence = context.packageManager.getApplicationLabel(ai)
            StringBuilder(charSequence.length).append(charSequence).toString()
        } else {
            "image_gallery_saver"
        }
        return appName
    }


}
