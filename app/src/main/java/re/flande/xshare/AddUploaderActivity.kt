package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewStub
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import java.net.URLDecoder

class AddUploaderActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_uploader)
    }

    override fun onStart() {
        super.onStart()

        Fuel.get("$GITHUB_APIURL/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/contents/")
                .header(mapOf("Accept" to "application/vnd.github.v$GITHUB_APIVER+json"))
                .responseJson { _, _, result ->
                    findViewById(R.id.progressBar).fadeOut()

                    when (result) {
                        is Result.Failure -> {
                            val layout = findViewById(R.id.addUploaderLayout)
                            val lv = findViewById(R.id.uploader_list) as ListView
                            val stub = findViewById(R.id.viewStub) as ViewStub?

                            stub ?: return@responseJson

                            lv.visibility = View.GONE
                            stub.setOnInflateListener { _, inflated ->
                                val tv = inflated.findViewById(R.id.errorText) as TextView
                                val butt = inflated.findViewById(R.id.tryAgain) as Button

                                tv.text = "${result.error}"
                                butt.setOnClickListener { recreate() }
                            }
                            stub.inflate()
                        }
                        is Result.Success -> {
                            val data = result.value.array()
                            val fileNames = ArrayList<String>()
                            val fileUrls = ArrayList<String>()

                            for (i in 0..(data.length() - 1)) {
                                val file = data.getJSONObject(i)

                                if (!file.getString("name").endsWith(".sxcu"))
                                    continue

                                fileNames.add(file.getString("name").removeSuffix(".sxcu"))
                                fileUrls.add(file.getString("download_url"))

                                Log.v(TAG, "host ${file.getString("name")}, url ${file.getString("download_url")}")
                            }

                            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
                            val lv = findViewById(R.id.uploader_list) as ListView
                            lv.adapter = adapter
                            lv.setOnItemClickListener { _, _, position, _ ->
                                downloadFile(fileUrls[position])
                            }
                        }
                    }
                }
    }

    fun downloadFile(addr: String) {
        val name = URLDecoder.decode(addr.split('/').last(), "UTF-8")

        Fuel.get(addr)
                .response { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            AlertDialog.Builder(this)
                                    .setMessage(resources.getString(R.string.failed_to_add, name, result.error))
                                    .setPositiveButton(android.R.string.ok, { _, _ -> })
                                    .show()
                        }
                        is Result.Success -> {
                            val intent = Intent(this, ImportActivity::class.java)
                                    .putExtra("name", name)
                                    .putExtra("contents", result.value)
                                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                }
    }
}
