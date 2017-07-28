package re.flande.xshare

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.google.gson.Gson
import java.io.File

class ShareActivity : Activity() {
    val REQUESTPERMS_CODE = 0

    var uploadCallback: () -> Unit = { throw AssertionError("no upload callback") }
    lateinit var errDialogBuilder: AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        errDialogBuilder = getFatalDialogBuilder(this)
                .setTitle(R.string.unable_to_upload)
                .setPositiveButton(android.R.string.ok, { _, _ -> })

        // Ideally we would filter out intents without an EXTRA_STREAM in the manifest, but it's
        // not possible. android.intent.category.OPENABLE is said to prevent receiving intents
        // without openable streams, but it doesn't work as intended.
        if (!intent.extras.containsKey(Intent.EXTRA_STREAM)) {
            errDialogBuilder.setMessage(R.string.no_extra_stream)
                    .show()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var uploaderName = prefs.getString("uploader", null)
        val uris: Iterable<Uri>

        if (intent.action == Intent.ACTION_SEND) {
            uris = arrayListOf(intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM))
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        } else {
            errDialogBuilder.setMessage(resources.getString(R.string.intent_action_not_supported, intent.action))
                    .show()
            return
        }

        if (uploaderName == null) {
            Log.d(TAG, "no default uploader set, using $DEFAULT_UPLOADER_FILENAME")

            File(getExternalFilesDir(null), DEFAULT_UPLOADER_FILENAME).outputStream().use { out ->
                resources.openRawResource(DEFAULT_UPLOADER_RESOURCE).use {
                    it.copyTo(out)
                }
            }

            prefs.edit().putString("uploader", DEFAULT_UPLOADER_FILENAME).apply()
            uploaderName = DEFAULT_UPLOADER_FILENAME
        }

        try {
            val uploader = getUploader(this, uploaderName)
            uploadCallback = { doUploads(uploader, uris) }
        } catch (e: Exception) {
            errDialogBuilder.setMessage(e.message).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && uris.any { it.scheme == "file" })
            requestPerms()
        else
            uploadCallback()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults ?: return

        for (res in grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                errDialogBuilder.setMessage(resources.getString(R.string.no_storage_permission))
                        .show()
                return
            }
        }

        uploadCallback()
    }

    private fun getUploader(context: Context, name: String): Uploader {
        File(context.getExternalFilesDir(null), name).inputStream().use {
            val up = Gson().fromJson(it.reader(), Uploader::class.java)
            return up
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPerms() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUESTPERMS_CODE)
        else
            uploadCallback()
    }

    fun doUploads(uploader: Uploader, uris: Iterable<Uri>) {
        uris.forEach {
            uploadFile(this, uploader, it)
        }

        finishAffinity()
    }
}
