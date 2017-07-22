package re.flande.xshare

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
    }

    fun updateInfo() {
        val current = prefs!!.getString("uploader", null)
        val sb = StringBuilder("Default uploader: $current\nUploaders: ")
        val files = this.filesDir.listFiles()
        for(i in files.indices) {
            sb.append(files[i].name)

            if(i < files.count() - 1)
                sb.append(", ")
        }

        val uptv = findViewById(R.id.uploaders_text) as TextView
        uptv.text = sb
    }

    override fun onStart() {
        super.onStart()

        updateInfo()
        val installSampleButton = findViewById(R.id.button) as Button
        installSampleButton.setOnClickListener {
            openFileOutput("uguu.sxcu", Context.MODE_PRIVATE).use { f ->
                f.write("""{
  "Name": "uguu.se",
  "DestinationType": "None",
  "RequestType": "POST",
  "RequestURL": "https://uguu.se/api.php?d=upload-tool",
  "FileFormName": "file",
  "Arguments": {
    "name": "",
    "randomname": ""
  },
  "ResponseType": "Text"
}""".toByteArray())
            }
            updateInfo()
        }

        val changeDefaultButton = findViewById(R.id.button2) as Button
        changeDefaultButton.setOnClickListener {
            val current = prefs!!.getString("uploader", null)
            val files = this.filesDir.listFiles()
            var new = current

            for(i in files.indices) {
                if(i > 0 && i == files.count() - 1) {
                    new = files[i-1].name
                    break
                }

                if(i < files.count() - 1 && files[i].name == current) {
                    new = files[i+1].name
                    break
                }
            }

            val editor = prefs!!.edit()
            editor?.putString("uploader", new)
            editor?.commit()
            updateInfo()
        }
    }
}
