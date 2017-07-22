package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
    }

    fun updateInfo() {
        val current = prefs!!.getString("uploader", null)
        val sb = StringBuilder(resources.getString(R.string.default_uploader))
        sb.append("<b>")
        sb.append(current)
        sb.append("</b><br />")
        sb.append(resources.getString(R.string.uploaders))

        val files = getExternalFilesDir(null).listFiles()
        for(i in files.indices) {
            sb.append(files[i].name)

            if(i < files.count() - 1)
                sb.append(", ")
        }

        val uptv = findViewById(R.id.uploaders_text) as TextView
        uptv.text = Html.fromHtml(sb.toString())
    }

    override fun onStart() {
        super.onStart()

        updateInfo()
        val installSampleButton = findViewById(R.id.button) as Button
        installSampleButton.setOnClickListener {
            File(getExternalFilesDir(null), "uguu.se.sxcu").outputStream().use { out ->
                val in_ = resources.openRawResource(R.raw.uguu)
                Util.copy(in_, out)
            }
            updateInfo()
        }

        val changeDefaultButton = findViewById(R.id.button2) as Button
        changeDefaultButton.setOnClickListener {
            val current = prefs!!.getString("uploader", null)
            val files = getExternalFilesDir(null).listFiles()
            var new = current

            for(i in files.indices) {
                if(i == files.count() - 1) {
                    new = files[0].name
                    break
                } else if(files[i].name == current) {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        item ?: return false

        if(item.itemId == R.id.action_add)
            startActivity(Intent(this, AddUploaderActivity::class.java))
        if(item.itemId == R.id.action_opendir) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.fromFile(getExternalFilesDir(null))
            intent.setDataAndType(uri, "resource/folder")
            try {
                startActivity(intent)
            } catch(e: ActivityNotFoundException) {
                AlertDialog.Builder(this)
                        .setMessage(resources.getString(R.string.no_file_managers, uri.path))
                        .setPositiveButton(android.R.string.ok, {_, _ ->})
                        .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
