package re.flande.xshare

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import java.io.File

class ShareActivity : Activity() {
    private val REQUESTPERMS_CODE = 0

    lateinit var errDialogBuilder: AlertDialog.Builder
    lateinit var uploader: Uploader
    lateinit var uris: Iterable<Uri>

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

        when (intent.action) {
            Intent.ACTION_SEND -> uris = arrayListOf(intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)
            else -> {
                errDialogBuilder.setMessage(resources.getString(R.string.intent_action_not_supported, intent.action))
                        .show()
                return
            }
        }

        if (uploaderName == null) {
            Log.d(TAG, "no default uploader set, using $DEFAULT_UPLOADER_FILENAME")

            try {
                File(getExternalFilesDir(null), DEFAULT_UPLOADER_FILENAME).outputStream().use { out ->
                    resources.openRawResource(DEFAULT_UPLOADER_RESOURCE).use {
                        it.copyTo(out)
                    }
                }
            } catch (e: Exception) {
                errDialogBuilder.setMessage(e.message).show()
                return
            }

            prefs.edit().putString("uploader", DEFAULT_UPLOADER_FILENAME).apply()
            uploaderName = DEFAULT_UPLOADER_FILENAME
        }

        try {
            File(getExternalFilesDir(null), uploaderName).inputStream().use {
                uploader = Uploader.fromInputStream(it)
            }
        } catch (e: Exception) {
            errDialogBuilder.setMessage(e.message).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && uris.any { it.scheme == "file" })
            requestPerms()
        else
            doUploads()
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

        doUploads()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPerms() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUESTPERMS_CODE)
        else
            doUploads()
    }

    private fun doUploads() {
        uris.forEach {
            uploadFile(this, uploader, it)
        }

        finishAffinity()
    }
}
