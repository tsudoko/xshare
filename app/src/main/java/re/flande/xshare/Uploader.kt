package re.flande.xshare

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Blob
import com.github.kittinunf.fuel.core.Method
import com.google.gson.Gson
import java.io.File
import java.io.FileNotFoundException

class Uploader : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "uploading")
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val notifID = (Math.random() * 1000000000.0).toInt() // FIXME: there's a slim possibility of a collision
        Log.d(TAG, "notifID $notifID")

        val uploader = intent.extras.getString("uploader")
        val file = intent.extras.getParcelable<Uri>("file") ?: throw AssertionError("no file specified")

        if (uploader == null) {
            Toast.makeText(this, R.string.no_uploader_set, Toast.LENGTH_SHORT).show()
            return
        }

        val blob = blobFromUri(file)
        val config = getConfig(uploader)

        if (config == null) {
            Toast.makeText(this, resources.getString(R.string.thing_not_found, uploader), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        var rurl = config.RequestURL ?: throw Exception("no uploader url specified")
        if (!rurl.startsWith("http"))
            rurl = "http://" + rurl

        val nBuilder = Notification.Builder(this)
                .setContentTitle(blob.name)
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
        notifManager.notify(notifID, nBuilder.build())

        var time = SystemClock.uptimeMillis()

        Fuel.upload(rurl, Method.valueOf(config.RequestType ?: "POST"), config.Arguments?.toList())
                .header(config.Headers)
                .name { config.FileFormName }
                .blob { _, _ -> blob }
                .progress { read, total ->
                    //Log.d(TAG, "read $read total $total")
                    // .progress gets called once with read=$total, total=0 before resolving the hostname for reasons unknown to me
                    if (total == 0L)
                        return@progress

                    // avoid slowing down the system with excessive notifications
                    val curTime = SystemClock.uptimeMillis()
                    if (curTime - time < 100)
                        return@progress
                    else
                        time = curTime

                    val p: Int = (read * 100 / total).toInt()
                    nBuilder.setContentText("$p%")
                            .setProgress(100, p, false)
                    notifManager.notify(notifID, nBuilder.build())
                }
                .interrupt {
                    // unfortunately this function isn't called when the app is killed, need to find some other way
                    notifManager.cancel(notifID)
                }
                .responseString { _, _, (d, err) ->
                    nBuilder.setProgress(0, 0, false)
                            .setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    notifManager.cancel(notifID)
                    val notifID = notifID + 1 // FIXME collisions

                    if (err != null || d == null) {
                        nBuilder.setContentTitle(blob.name)
                                .setContentText(resources.getString(R.string.upload_failed))
                                .setStyle(Notification.BigTextStyle().bigText(err.toString()))
                        notifManager.notify(notifID, nBuilder.build())
                    } else {
                        val url = config.prepareUrl(d)
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        val intent = PendingIntent.getActivity(this, 0, i, 0)
                        nBuilder.setContentTitle(blob.name)
                                .setContentText(url)
                                .setStyle(Notification.BigTextStyle().bigText(url))
                                .setContentIntent(intent)
                        notifManager.notify(notifID, nBuilder.build())

                        if (prefs.getBoolean("autoclip", false))
                            clipManager.primaryClip = ClipData.newPlainText("URL", url)
                    }
                }
        finish()
    }

    fun blobFromUri(uri: Uri): Blob {
        contentResolver.query(uri, null, null, null, null).use { cursor ->
            cursor.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

            contentResolver.openFileDescriptor(uri, "r").use { fd ->
                return Blob(name, fd.statSize, { contentResolver.openInputStream(uri) })
            }
        }
    }

    fun getConfig(uploader: String): Config? {
        try {
            File(getExternalFilesDir(null), uploader).inputStream().use {
                return Gson().fromJson(it.reader(), Config::class.java)
            }
        } catch(e: FileNotFoundException) {
            return null
        }
    }
}
