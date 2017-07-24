package re.flande.xshare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager

class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val uploader = prefs.getString("uploader", null)

        if(!intent.extras.containsKey(Intent.EXTRA_STREAM))
            return

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
        val intent = Intent(applicationContext, Uploader::class.java)
        intent.putExtra("uploader", uploader)
        intent.putExtra("file", uri)
        intent.flags = Intent.FLAG_ACTIVITY_NO_USER_ACTION
        startActivity(intent)
    }
}
