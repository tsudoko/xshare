package re.flande.xshare

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem

class MainActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onStart() {
        super.onStart()

        val files = getExternalFilesDir(null).listFiles()
                .filter( {it.name.endsWith(".sxcu") })
                .map({ it.name })

        val uploaderPref = findPreference("uploader") as ListPreference
        uploaderPref.entries = files.map { it.removeSuffix(".sxcu") }.toTypedArray()
        uploaderPref.entryValues = files.toTypedArray()
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
