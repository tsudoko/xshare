package re.flande.xshare

import android.app.Activity
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

    val RES_UPLOADER_ADD = 0

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
                val menu = PopupMenu(this, findViewById(R.id.action_add))
                menu.inflate(R.menu.menu_add_uploader)
                menu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_addfrom -> {
                            val menu = PopupMenu(this, findViewById(R.id.action_addfrom) ?: findViewById(R.id.action_add))
                            menu.inflate(R.menu.menu_add_uploader_from)
                            menu.setOnMenuItemClickListener { addFromOnItemClickListener(it) }
                            menu.show()
                        }
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

    private fun addFromOnItemClickListener(item: MenuItem): Boolean {
        // TODO: uploaders aren't reloaded immediately (e.g. after addfromclip)
        when (item.itemId) {
            R.id.action_addfromclip -> {
                val clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipManager.primaryClip

                if (clip == null) {
                    AlertDialog.Builder(this)
                            .setMessage(R.string.clipboard_empty)
                            .setPositiveButton(android.R.string.ok, { _, _ -> })
                            .show()
                    return false
                }

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
            R.id.action_addfromfile -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*")
                startActivityForResult(intent, RES_UPLOADER_ADD)
            }
            R.id.action_addfromsample -> startActivity(Intent(this, ChooseSampleActivity::class.java))
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED)
            return

        if (resultCode != Activity.RESULT_OK || data == null) {
            AlertDialog.Builder(this)
                    .setMessage("some error here, result $resultCode data null ${data == null}")
                    .show()
            return
        }

        when (requestCode) {
            RES_UPLOADER_ADD -> {
                val intent = Intent(this, ImportActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData(data.data)
                startActivity(intent)
            }
        }
    }
}
