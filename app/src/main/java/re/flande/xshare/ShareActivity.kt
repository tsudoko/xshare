package re.flande.xshare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class ShareActivity : Activity() {
    val LASTFILE = 199

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent.action == Intent.ACTION_SEND) {
            if(!intent.extras.containsKey(Intent.EXTRA_STREAM))
                return

            val fileUri = intent.extras.getParcelable<Uri>(Intent.EXTRA_STREAM)

            val intent = Intent(this, Uploader::class.java)
            intent.putExtra("uploader", "goud")
            intent.putExtra("file", fileUri)
            startActivityForResult(intent, LASTFILE)
        } else if(intent.action == Intent.ACTION_SEND_MULTIPLE) {
            if(!intent.extras.containsKey(Intent.EXTRA_STREAM))
                return

            val uris = intent.extras.getParcelableArrayList<Uri>(Intent.EXTRA_STREAM)

            for(i in uris.indices) {
                val intent = Intent(this, Uploader::class.java)
                intent.putExtra("uploader", "goud")
                intent.putExtra("file", uris[i])

                if(i == uris.count() - 1)
                    startActivityForResult(intent, LASTFILE)
                else
                    startActivity(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == LASTFILE)
            finish()
    }
}
