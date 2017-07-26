package re.flande.xshare

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
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
                    .setPositiveButton(R.string.proceed_anyway, { _, _ -> import(name + ".sxcu", in_) })
                    .setNegativeButton(android.R.string.cancel, { _, _ -> })
                    .show()
        } else {
            import(name, in_)
        }
    }

    fun import(name: String, inStream: InputStream) {
        val f = File(getExternalFilesDir(null), name)

        if(f.exists()) {
            getFatalDialogBuilder(this)
                    .setMessage(resources.getString(R.string.thing_already_exists, f.name))
                    .setPositiveButton(R.string.proceed_anyway, { _, _ -> importFile(inStream, f) })
                    .setNegativeButton(android.R.string.cancel, { _, _ -> })
                    .show()
        } else {
            importFile(inStream, f)
        }
    }

    private fun importFile(inStream: InputStream, outFile: File) {
        // TODO: config validation
        inStream.use {
            outFile.outputStream().use { out ->
                inStream.copyTo(out)
            }
        }

        Toast.makeText(this, resources.getString(R.string.thing_added, outFile.name), Toast.LENGTH_SHORT).show()
        finishAffinity()
    }
}
