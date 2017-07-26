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

class ShareActivity : Activity() {
    val REQUESTPERMS_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ideally we would filter out intents without an EXTRA_STREAM in the manifest, but it's
        // not possible. android.intent.category.OPENABLE is said to prevent receiving intents
        // without openable streams, but it doesn't work as intended.
        if(!intent.extras.containsKey(Intent.EXTRA_STREAM)) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.unable_to_upload)
                    .setMessage(R.string.no_extra_stream)
                    .setPositiveButton(android.R.string.ok, { _, _ -> })
                    .setOnDismissListener { finishAffinity() }
                    .show()
            return
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPerms()
        else
            doUploads()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        grantResults ?: return

        for(res in grantResults) {
            if(res != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.unable_to_upload))
                        .setMessage(resources.getString(R.string.no_storage_permission))
                        .setPositiveButton(android.R.string.ok, { _, _ -> })
                        .setOnDismissListener { finishAffinity() }
                        .show()
                return
            }
        }

        doUploads()
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPerms() {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUESTPERMS_CODE)
        else
            doUploads()
    }

    fun doUploads() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val uploader = prefs.getString("uploader", null)

        if(intent.action == Intent.ACTION_SEND) {
            val fileUri = intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM)

            uploadFile(this, uploader, fileUri)
        } else if(intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)

            for(u in uris)
                uploadFile(this, uploader, u)
        }

        finishAffinity()
    }
}
