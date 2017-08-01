package re.flande.xshare

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu

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
            R.id.action_add -> {
                // TODO: uploaders aren't reloaded immediately (e.g. after addfromclip)
                val menu = PopupMenu(this, findViewById(R.id.action_add))
                menu.inflate(R.menu.menu_add_uploader)
                menu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_addfromclip -> {
                            val clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipManager.primaryClip

                            val intent = Intent(this, ImportActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            (0..clip.itemCount - 1).map { clip.getItemAt(it) }.forEach {
                                if (it.text != null) {
                                    intent.putExtra("contents", it.text.toString().toByteArray()) // FIXME shouldn't need conversion
                                    return@forEach
                                } else if (it.uri != null) {
                                    intent.data = it.uri
                                    return@forEach
                                }
                            }

                            startActivity(intent)
                        }
                        R.id.action_addfromsample -> startActivity(Intent(this, ChooseSampleActivity::class.java))
                    }

                    true
                }
                menu.show()
            }
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
