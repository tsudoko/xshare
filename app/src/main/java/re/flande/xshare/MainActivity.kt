package re.flande.xshare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
    }

    fun updateInfo() {
        val current = prefs!!.getString("uploader", null)
        val sb = StringBuilder(resources.getString(R.string.default_uploader))
        sb.append("<b>")
        sb.append(current)
        sb.append("</b><br />")
        sb.append(resources.getString(R.string.uploaders))

        val files = this.filesDir.listFiles()
        for(i in files.indices) {
            sb.append(files[i].name)

            if(i < files.count() - 1)
                sb.append(", ")
        }

        val uptv = findViewById(R.id.uploaders_text) as TextView
        uptv.text = Html.fromHtml(sb.toString())
    }

    override fun onStart() {
        super.onStart()

        updateInfo()
        val installSampleButton = findViewById(R.id.button) as Button
        installSampleButton.setOnClickListener {
            openFileOutput("uguu.sxcu", Context.MODE_PRIVATE).use { out ->
                val in_ = resources.openRawResource(R.raw.uguu)
                Util.copy(in_, out)
            }
            updateInfo()
        }

        val changeDefaultButton = findViewById(R.id.button2) as Button
        changeDefaultButton.setOnClickListener {
            val current = prefs!!.getString("uploader", null)
            val files = this.filesDir.listFiles()
            var new = current

            for(i in files.indices) {
                if(files.count() == 1)
                    new = files[i].name

                if(i > 0 && i == files.count() - 1) {
                    new = files[i-1].name
                    break
                }

                if(i < files.count() - 1 && files[i].name == current) {
                    new = files[i+1].name
                    break
                }
            }

            val editor = prefs!!.edit()
            editor?.putString("uploader", new)
            editor?.commit()
            updateInfo()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        item ?: return false

        if(item.itemId == R.id.action_add)
            startActivity(Intent(this, AddUploaderActivity::class.java))

        return super.onOptionsItemSelected(item)
    }
}
