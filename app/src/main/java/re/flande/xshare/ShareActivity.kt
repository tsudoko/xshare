package re.flande.xshare

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import java.io.File

class ShareActivity : Activity() {
    val REQUESTPERMS_CODE = 0

    var uploadCallback: () -> Unit = { throw AssertionError("no upload callback") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ideally we would filter out intents without an EXTRA_STREAM in the manifest, but it's
        // not possible. android.intent.category.OPENABLE is said to prevent receiving intents
        // without openable streams, but it doesn't work as intended.
        if (!intent.extras.containsKey(Intent.EXTRA_STREAM)) {
            getFatalDialogBuilder(this)
                    .setTitle(R.string.unable_to_upload)
                    .setMessage(R.string.no_extra_stream)
                    .setPositiveButton(android.R.string.ok, { _, _ -> })
                    .show()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val uploader = prefs.getString("uploader", null)
        val uris: Iterable<Uri>

        if (intent.action == Intent.ACTION_SEND) {
            uris = arrayListOf(intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM))
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)
        } else {
            getFatalDialogBuilder(this)
                    .setTitle(R.string.unable_to_upload)
                    .setMessage(resources.getString(R.string.intent_action_not_supported, intent.action))
                    .setPositiveButton(android.R.string.ok, { _, _ -> })
                    .show()
            return
        }

        if (uploader != null) {
            uploadCallback = { doUploads(uploader, uris) }
        } else {
            Log.d(TAG, "no default uploader set, using $DEFAULT_UPLOADER_FILENAME")

            File(getExternalFilesDir(null), DEFAULT_UPLOADER_FILENAME).outputStream().use { out ->
                resources.openRawResource(DEFAULT_UPLOADER_RESOURCE).use {
                    it.copyTo(out)
                }
            }

            prefs.edit().putString("uploader", DEFAULT_UPLOADER_FILENAME).apply()
            uploadCallback = { doUploads(DEFAULT_UPLOADER_FILENAME, uris) }
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
                getFatalDialogBuilder(this)
                        .setTitle(resources.getString(R.string.unable_to_upload))
                        .setMessage(resources.getString(R.string.no_storage_permission))
                        .setPositiveButton(android.R.string.ok, { _, _ -> })
                        .show()
                return
            }
        }

        uploadCallback()
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPerms() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUESTPERMS_CODE)
        else
            uploadCallback()
    }

    fun doUploads(uploader: String, uris: Iterable<Uri>) {
        uris.forEach {
            uploadFile(this, uploader, it)
        }

        finishAffinity()
    }
}
