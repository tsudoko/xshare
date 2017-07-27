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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

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
        for (i in files.indices) {
            sb.append(files[i].name.removeSuffix(".sxcu"))

            if (i < files.count() - 1)
                sb.append(", ")
        }

        val uptv = findViewById(R.id.uploaders_text) as TextView
        uptv.text = Html.fromHtml(sb.toString())
    }

    override fun onStart() {
        super.onStart()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        updateInfo(prefs)

        val changeDefaultButton = findViewById(R.id.button2) as Button
        changeDefaultButton.setOnClickListener {
            val files = getExternalFilesDir(null).listFiles().map({ it.name.removeSuffix(".sxcu") })
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, files)

            AlertDialog.Builder(this)
                    .setAdapter(adapter, { _, i ->
                        val editor = prefs.edit()
                        editor?.putString("uploader", files[i] + ".sxcu")
                        editor?.commit()
                        updateInfo(prefs)
                    })
                    .show()
        }

        val autoclipSwitch = findViewById(R.id.switchAutoclip) as Switch
        autoclipSwitch.setOnClickListener {
            val editor = prefs.edit()
            editor?.putBoolean("autoclip", (it as Switch).isChecked)
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

        itemSelect@ when (item.itemId) {
            R.id.action_add ->
                startActivity(Intent(this, AddUploaderActivity::class.java))
            R.id.action_opendir -> {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.fromFile(getExternalFilesDir(null))
                val mimeTypes = arrayOf(
                        "resource/folder",
                        "vnd.android.document/directory",
                        "vnd.android.cursor.item/file",
                        "inode/directory",
                        "x-directory/normal"
                )

                for (type in mimeTypes) {
                    intent.setDataAndType(uri, type)
                    try {
                        startActivity(intent)
                        Log.d(TAG, "matching directory type: $type")
                        return@itemSelect
                    } catch(e: ActivityNotFoundException) {
                    }
                }

                AlertDialog.Builder(this)
                        .setMessage(resources.getString(R.string.no_file_managers, uri.path))
                        .setPositiveButton(android.R.string.ok, { _, _ -> })
                        .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
