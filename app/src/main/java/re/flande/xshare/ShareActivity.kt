package re.flande.xshare

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
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
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notif = Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(resources.getString(R.string.unable_to_upload))
                        .setContentText(resources.getString(R.string.no_storage_permission))
                        .build()
                notifManager.notify(0, notif)

                finishAffinity()
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

            upload(fileUri, uploader)
        } else if(intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)

            for(u in uris)
                upload(u, uploader)
        }

        finishAffinity()
    }

    fun upload(uri: Uri, uploader: String) {
        val intent = Intent(this, Uploader::class.java)
                .putExtra("uploader", uploader)
                .putExtra("file", uri)
                .addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        startActivity(intent)
    }
}
