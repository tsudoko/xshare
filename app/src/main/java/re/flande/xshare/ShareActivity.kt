package re.flande.xshare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log

class ShareActivity : Activity() {
    var uploads = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val uploader = prefs.getString("uploader", null)

        if(intent.action == Intent.ACTION_SEND) {
            if(!intent.extras.containsKey(Intent.EXTRA_STREAM))
                return

            val fileUri = intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM)

            val intent = Intent(this, Uploader::class.java)
            intent.putExtra("uploader", uploader)
            intent.putExtra("file", fileUri)
            startActivityForResult(intent, 0)
            uploads++
        } else if(intent.action == Intent.ACTION_SEND_MULTIPLE) {
            if(!intent.extras.containsKey(Intent.EXTRA_STREAM))
                return

            val uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)

            for(i in uris.indices) {
                val intent = Intent(this, Uploader::class.java)
                intent.putExtra("uploader", uploader)
                intent.putExtra("file", uris[i])

                startActivityForResult(intent, 0)
                uploads++
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("Xshare", "$uploads uploads, removing one")
        uploads--
        if(uploads == 0) {
            Log.d("Xshare", "last file, bailing out")
            finishAffinity()
        }
    }
}
