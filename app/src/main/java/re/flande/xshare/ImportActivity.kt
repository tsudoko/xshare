package re.flande.xshare

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.google.gson.Gson
import java.io.File
import java.io.InputStream

class ImportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contents = intent.extras?.getByteArray("contents")

        val name: String
        val in_: InputStream

        if(contents == null) {
            name = intent.data.getFilename(this)
            in_ = contentResolver.openInputStream(intent.data)
        } else {
            name = intent.extras.getString("name")
            in_ = contents.inputStream()
        }

        if (name.split('.').last() != "sxcu") {
            getFatalDialogBuilder(this)
                    .setMessage(R.string.file_not_sxcu)
                    .setPositiveButton(R.string.proceed_anyway, { _, _ -> import(in_) })
                    .setNegativeButton(android.R.string.cancel, { _, _ -> })
                    .show()
        } else {
            import(in_)
        }
    }

    fun import(inStream: InputStream) {
        try {
            val up = makeUploader(inStream)
            val name = (up.Name + ".sxcu").replace("/", "")
            val f = File(getExternalFilesDir(null), name)

            if(f.exists()) {
                getFatalDialogBuilder(this)
                        .setMessage(resources.getString(R.string.thing_already_exists, up.Name))
                        .setPositiveButton(R.string.proceed_anyway, { _, _ -> importFile(up, f) })
                        .setNegativeButton(android.R.string.cancel, { _, _ -> })
                        .show()
            } else {
                importFile(up, f)
            }
        } catch (e: Exception) {
            getFatalDialogBuilder(this)
                    .setTitle(R.string.unable_to_import)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.ok, { _, _ -> })
        }
    }

    private fun makeUploader(inStream: InputStream): Uploader {
        inStream.use {
            it.reader().use {
                val uploader = Gson().fromJson(it, Uploader::class.java)
                uploader.validate()
                return uploader
            }
        }
    }

    private fun importFile(up: Uploader, f: File) {
        Gson().toJson(up, f.writer())

        Toast.makeText(this, resources.getString(R.string.thing_added, up.Name), Toast.LENGTH_SHORT).show()
        finishAffinity()
    }
}
