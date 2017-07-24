package re.flande.xshare

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle

class ImportActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pd = ProgressDialog.show(this, "", resources.getString(R.string.importing))
        contentResolver.openInputStream(intent.data).use { in_ ->
            val name = intent.data.path.split("/").last()
            openFileOutput(name, Context.MODE_PRIVATE).use { out ->
                copy(in_, out)
            }
        }
        // TODO: config validation
        pd.cancel()
        finishAffinity()
    }
}
