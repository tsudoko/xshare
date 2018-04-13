package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.gson.GsonBuilder
import java.io.File
import java.io.InputStream

class ImportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contents = intent.extras?.getByteArray("contents")

        val name: String?
        val in_: InputStream

        when {
            intent.data != null -> {
                name = intent.data.getFilename(this)
                in_ = contentResolver.openInputStream(intent.data)
            }
            contents != null -> {
                name = intent.extras.getString("name")
                in_ = contents.inputStream()
            }
            else -> {
                fail(Exception(resources.getString(R.string.nothing_to_import)))
                return
            }
        }

        if (name != null && name.split('.').last() != "sxcu") {
            val d = getFatalDialogBuilder(this)
                    .setMessage(R.string.file_not_sxcu)
                    .setPositiveButton(R.string.proceed_anyway, null) // prevents calling d.dismiss()
                    .setNegativeButton(android.R.string.cancel, { _, _ -> })
                    .create()

            d.setOnShowListener {
                d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { importStream(in_); d.hide() }
            }
            d.show()
        } else {
            importStream(in_)
        }
    }

    private fun import(up: Uploader) = try {
        val name = (up.Name + ".sxcu").replace("/", "⁄")
        val f = File(getExternalFilesDir(null), name)

        if (f.exists()) {
            val d = getFatalDialogBuilder(this)
                    .setMessage(resources.getString(R.string.thing_already_exists, up.Name))
                    .setPositiveButton(R.string.overwrite, { _, _ -> uploaderToFile(up, f) })
                    .setNeutralButton(R.string.rename, null)
                    .setNegativeButton(android.R.string.cancel, { _, _ -> })
                    .create()

            d.setOnShowListener {
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { rename(up); d.hide() }
            }
            d.show()
        } else {
            uploaderToFile(up, f)
        }
    } catch (e: Exception) {
        fail(e)
    }

    private fun importStream(inStream: InputStream) = try {
        val up = Uploader.fromInputStream(inStream)
        import(up)
    } catch (e: Exception) {
        fail(e)
    }

    private fun fail(e: Exception) {
        Log.d(TAG, "fail()'d, stack trace below")
        e.printStackTrace()
        getFatalDialogBuilder(this)
                .setTitle(R.string.unable_to_import)
                .setMessage(e.localizedMessage ?: e.toString())
                .setPositiveButton(android.R.string.ok, { _, _ -> })
                .show()
    }

    private fun rename(up: Uploader) {
        // FIXME: the IME doesn't get hidden when cancelled
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val edit = EditText(this)
        edit.setSingleLine()
        edit.hint = resources.getString(R.string.name)
        edit.append(up.Name)

        val d = getFatalDialogBuilder(this)
                .setView(edit)
                .setPositiveButton(R.string.rename, null)
                .setNegativeButton(android.R.string.cancel, { _, _ -> })
                .create()

        val onDone = {
            up.Name = edit.text.toString()
            import(up)
            d.hide()
            imm.hideSoftInputFromWindow(edit.windowToken, 0)
        }

        edit.setOnEditorActionListener { _, _, _ -> onDone(); true }
        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onDone() }
            imm.showSoftInput(edit, InputMethodManager.SHOW_FORCED)
        }
        d.show()
    }

    private fun uploaderToFile(up: Uploader, f: File) {
        f.writer().use {
            it.write(GsonBuilder().setPrettyPrinting().create().toJson(up))
        }

        Toast.makeText(this, resources.getString(R.string.thing_added, up.Name), Toast.LENGTH_SHORT).show()
        finishAffinity()
    }
}
