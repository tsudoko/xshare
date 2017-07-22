package re.flande.xshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openFileOutput("goud.sxcu", Context.MODE_PRIVATE).use { f ->
            f.write("""{
  "Name": "yuri.fun",
  "DestinationType": "None",
  "RequestType": "POST",
  "RequestURL": "a.yuri.fun/upload.php",
  "FileFormName": "files[]",
  "ResponseType": "Text",
  "URL": "${'$'}json:files[0].url${'$'}"
}""".toByteArray())
        }
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val butt = findViewById(R.id.button) as Button
        butt.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("*/*")
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1)
            finishAffinity()

        if(resultCode != RESULT_OK)
            return

        val intent = Intent(this, Uploader::class.java)
        intent.putExtra("uploader", "goud")
        intent.putExtra("file", data?.data)
        startActivityForResult(intent, 1)
    }
}
