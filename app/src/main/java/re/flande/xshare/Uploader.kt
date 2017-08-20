package re.flande.xshare

import android.util.Log
import com.google.code.regexp.Pattern
import com.google.gson.Gson
import com.jayway.jsonpath.JsonPath
import org.xml.sax.InputSource
import java.io.InputStream
import javax.xml.xpath.XPathFactory

// ref: https://github.com/ShareX/ShareX/raw/master/ShareX.UploadersLib/Helpers/CustomUploaderItem.cs
class Uploader(var Name: String?,
               var DestinationType: String?,
               var RequestType: String?,
               var RequestURL: String?,
               var FileFormName: String?,
               var Headers: Map<String, String>?,
               var Arguments: Map<String, String>?,
               var RegexList: Array<String>?,
               var ResponseType: String?,
               var URL: String?) {

    class EmptyFieldException(val fieldName: String) : Exception("$fieldName must not be empty")

    fun validate() {
        if(RequestType.isNullOrEmpty())
            RequestType = "POST"
        if(RequestURL.isNullOrEmpty())
            throw EmptyFieldException("RequestURL")
        if(FileFormName.isNullOrEmpty())
            throw EmptyFieldException("FileFormName")
    }

    /*
     * Informal spec of the implemented query lang subset
     * "::=" assignments are BNF, "=" are regexps
     *
     * query ::= "$" [type ":"] actual-query "$"
     * type ::= "json" | "xml" | "random"
     * actual-query = [^$]*
     */
    fun prepareUrl(response: String): String {
        val rawResult = StringBuilder()
        var insideQuery = false
        var afterType = false
        var query = charArrayOf()
        var queryType = charArrayOf()
        var doQuery: ((String) -> String)? = null

        if (URL.isNullOrEmpty())
            return response

        for (c in URL!!.toCharArray()) {
            if (c == '$') {
                if (insideQuery && !afterType) {
                    query = queryType
                    queryType = "regex".toCharArray()
                }

                if (queryType.isNotEmpty()) {
                    when (String(queryType)) {
                        "json" -> doQuery = { JsonPath.read(response, it) }
                        "xml" -> doQuery = { XPathFactory.newInstance().newXPath().evaluate(it, InputSource(response.byteInputStream())) }
                        "regex" -> doQuery = { matchRegex(response, it) }
                        "random" -> doQuery = { it.split('|').getRandom() }
                        else -> throw IllegalStateException("query type ${String(queryType)} not implemented")
                    }
                }

                if (query.isNotEmpty()) {
                    if (doQuery == null)
                        throw AssertionError("no query handler to execute $query")

                    Log.d(TAG, "query ${String(query)}")
                    rawResult.append(doQuery(String(query)))
                }

                insideQuery = !insideQuery
                query = charArrayOf()
                queryType = charArrayOf()
                doQuery = null
                afterType = false

                continue
            }

            if (insideQuery) {
                if (c == ':' && !afterType) {
                    afterType = true
                    continue
                }

                if (afterType)
                    query += c
                else
                    queryType += c
            } else {
                rawResult.append(c)
            }
        }

        Log.d(TAG, "result $rawResult")
        return rawResult.toString()
    }

    private fun matchRegex(text: String, query: String): String {
        val reIndex = StringBuilder()
        var afterIndex = false
        val groupName = StringBuilder()

        for (c in query) {
            if (!afterIndex) {
                when {
                    c.isDigit() -> reIndex.append(c)
                    c == ',' -> afterIndex = true
                }
            } else {
                groupName.append(c)
            }
        }

        val i = Integer.parseInt(reIndex.toString()) - 1
        val matcher = Pattern.compile(RegexList?.get(i) ?: throw EmptyFieldException("RegexList")).matcher(text)
        matcher.find()

        if (groupName.isEmpty()) {
            return matcher.group()
        } else {
            val g = groupName.toString()
            try {
                return matcher.group(Integer.parseInt(g))
            } catch (_: NumberFormatException) {
                return matcher.group(g)
            }
        }
    }

    companion object {
        fun fromInputStream(stream: InputStream): Uploader {
            stream.use {
                it.reader().use {
                    val uploader = Gson().fromJson(it, Uploader::class.java)
                    uploader.validate()
                    return uploader
                }
            }
        }
    }
}
