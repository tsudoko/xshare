package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import java.io.File

class ImportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = getFilename(intent.data)
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

    fun getFilename(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null).use {
            it.moveToFirst()
            return it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }

    fun import(name: String) {
        val pd = ProgressDialog.show(this, "", resources.getString(R.string.importing))
        contentResolver.openInputStream(intent.data).use { in_ ->
            File(getExternalFilesDir(null), name).outputStream().use { out ->
                copy(in_, out)
            }
        }
        // TODO: config validation
        pd.cancel()
        Toast.makeText(this, resources.getString(R.string.added, name), Toast.LENGTH_SHORT).show()
        finishAffinity()
    }
}
