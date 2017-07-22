package re.flande.xshare

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.google.gson.Gson
import java.io.FileNotFoundException
import java.io.FileOutputStream

class Uploader : Activity() {
    val TAG = "Xshare"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "uploading")
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifID = (Math.random() * 1000000000.0).toInt() // FIXME: there's a slim possibility of a collision
        Log.d(TAG, "notifID $notifID")

        val uploader = intent.extras.getString("uploader") ?: throw AssertionError("no uploader specified")
        val file = intent.extras.getParcelable<Uri>("file")  ?: throw AssertionError("no file specified")

        Log.d(TAG, "path is ${file.path}")
        val path = file.path ?: throw AssertionError("null file path, fix your shit")
        val in_ = contentResolver.openInputStream(file)
        val tempFile = createTempFile()
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { out ->
            Util.copy(in_, out)
        }

        try {
            openFileInput(uploader).use { f ->
                val config = Gson().fromJson(f.reader(), Config::class.java)

                var rurl = config?.RequestURL ?: throw Exception("no uploader url specified")
                if (!rurl.startsWith("http"))
                    rurl = "http://" + rurl

                val nBuilder = Notification.Builder(this)
                        .setContentTitle(resources.getString(R.string.uploading_thing, path))
                        .setProgress(100, 0, true)
                        .setOngoing(true)
                        .setSmallIcon(R.mipmap.ic_launcher)

                notifManager.notify(notifID, nBuilder.build())

                var time = SystemClock.uptimeMillis()

                Fuel.upload(rurl, Method.valueOf(config.RequestType ?: "POST"), config.Arguments?.toList())
                        .header(config.Headers)
                        .source { req, _ ->
                            req.name = config.FileFormName
                            tempFile
                        }
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
                        .responseString { _, _, result ->
                            val (d, err) = result
                            nBuilder.setProgress(0, 0, false).setOngoing(false)
                            notifManager.cancel(notifID)
                            val notifID = notifID + 1 // FIXME collisions

                            if (err != null || d == null) {
                                nBuilder.setContentTitle(resources.getString(R.string.upload_failed))
                                        .setContentText(err.toString())
                                notifManager.notify(notifID, nBuilder.build())
                            } else {
                                val url = config.prepareUrl(d)
                                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                val intent = PendingIntent.getActivity(this, 0, i, 0)
                                nBuilder.setContentTitle(resources.getString(R.string.upload_successful))
                                        .setContentText(url)
                                        .setContentIntent(intent)
                                notifManager.notify(notifID, nBuilder.build())
                            }
                        }
            }
        } catch(e: FileNotFoundException) {
            Toast.makeText(this, resources.getString(R.string.thing_not_found, uploader), Toast.LENGTH_SHORT).show()
        } finally {
            parent?.setResult(RESULT_OK) ?: setResult(RESULT_OK)
            finish()
        }
    }

}
