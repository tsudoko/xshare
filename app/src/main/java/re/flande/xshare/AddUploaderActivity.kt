package re.flande.xshare

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result

class AddUploaderActivity : Activity() {
    val API_URL = "https://api.github.com"
    val API_VER = "3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_uploader)
    }

    override fun onStart() {
        super.onStart()

        Fuel.get("$API_URL/repos/${resources.getString(R.string.uploaders_repo_owner)}/${resources.getString(R.string.uploaders_repo_name)}/contents/")
                .header(mapOf("Accept" to "application/vnd.github.v$API_VER+json"))
                .responseJson { req, res, result ->
                    when(result) {
                        is Result.Failure -> {
                            // TODO: handle error
                            Log.d("Xshare", "something happened")
                            Log.d("Xshare", "${result.error}")
                        }
                        is Result.Success -> {
                            val data = result.value.array()
                            val fileNames = ArrayList<String>()
                            val fileUrls = ArrayList<String>()

                            for(i in 0..(data.length() - 1)) {
                                val file = data.getJSONObject(i)

                                if(!file.getString("name").endsWith(".sxcu"))
                                    continue

                                fileNames.add(file.getString("name").removeSuffix(".sxcu"))
                                fileUrls.add(file.getString("download_url"))

                                Log.d("Xshare", "host ${file.getString("name")}, url ${file.getString("download_url")}")
                            }

                            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames)
                            val lv = findViewById(R.id.uploader_list) as ListView
                            lv.adapter = adapter
                            lv.setOnItemClickListener { parent, view, position, id ->
                                downloadFile(fileUrls[position])
                            }
                        }
                    }
                }
    }

    fun downloadFile(addr: String) {
        val name = addr.split('/').last()

        Fuel.get(addr)
                .response { req, res, result ->
                    when(result) {
                        is Result.Failure -> {
                            Toast.makeText(this, resources.getString(R.string.failed_to_add, name, result.error), Toast.LENGTH_SHORT).show()
                        }
                        is Result.Success -> {
                            openFileOutput(name, Context.MODE_PRIVATE).use { f ->
                                f.write(result.value)
                            }
                            Toast.makeText(this, resources.getString(R.string.added, name), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
    }
}
