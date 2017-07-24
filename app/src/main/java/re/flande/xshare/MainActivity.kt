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
import android.widget.Switch
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    fun updateInfo(prefs: SharedPreferences) {
        val current = prefs.getString("uploader", null)
        val autoclip = prefs.getBoolean("autoclip", false)
        val clipSwitch = findViewById(R.id.switchAutoclip) as Switch
        clipSwitch.isChecked = autoclip

        val sb = StringBuilder(resources.getString(R.string.default_uploader))
        sb.append("<b>")
        sb.append(current?.removeSuffix(".sxcu"))
        sb.append("</b><br />")
        sb.append(resources.getString(R.string.uploaders))

        val files = getExternalFilesDir(null).listFiles()
        for(i in files.indices) {
            sb.append(files[i].name.removeSuffix(".sxcu"))

            if(i < files.count() - 1)
                sb.append(", ")
        }

        val uptv = findViewById(R.id.uploaders_text) as TextView
        uptv.text = Html.fromHtml(sb.toString())
    }

    override fun onStart() {
        super.onStart()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        updateInfo(prefs)

        val installSampleButton = findViewById(R.id.button) as Button
        installSampleButton.setOnClickListener {
            File(getExternalFilesDir(null), "uguu.se.sxcu").outputStream().use { out ->
                val in_ = resources.openRawResource(R.raw.uguu)
                copy(in_, out)
            }
            updateInfo(prefs)
        }

        val changeDefaultButton = findViewById(R.id.button2) as Button
        changeDefaultButton.setOnClickListener {
            val current = prefs.getString("uploader", null)
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

            val editor = prefs.edit()
            editor?.putString("uploader", new)
            editor?.commit()
            updateInfo(prefs)
        }

        val autoclipSwitch = findViewById(R.id.switchAutoclip) as Switch
        autoclipSwitch.setOnClickListener { v ->
            v as Switch
            val editor = prefs.edit()
            editor?.putBoolean("autoclip", v.isChecked)
            editor?.commit()
            updateInfo(prefs)
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
