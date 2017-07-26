package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.InputStream

class ImportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.data.getFilename(this)
        Log.d(TAG, "contentresolver name $name")

        if(name.split('.').last() != "sxcu") {
            AlertDialog.Builder(this)
                    .setMessage(R.string.file_not_sxcu)
                    .setPositiveButton(R.string.proceed_anyway, { _, _ -> import(name + ".sxcu") })
                    .setNegativeButton(android.R.string.cancel, { _, _ -> finish() })
                    .show()
        } else {
            import(name)
        }
    }

    fun import(name: String) {
        val in_ = contentResolver.openInputStream(intent.data)
        val f = File(getExternalFilesDir(null), name)

        if(f.exists()) {
            AlertDialog.Builder(this)
                    .setMessage(resources.getString(R.string.thing_already_exists, f.name))
                    .setPositiveButton(R.string.proceed_anyway, { _, _ -> addFile(in_, f) })
                    .setNegativeButton(android.R.string.cancel, { _, _ -> finish() })
                    .show()
        } else {
            addFile(in_, f)
        }
    }

    fun addFile(inStream: InputStream, outFile: File) {
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
