package re.flande.xshare

import android.util.Log
import com.jayway.jsonpath.JsonPath
import org.xml.sax.InputSource
import javax.xml.xpath.XPathFactory

// ref: https://github.com/ShareX/ShareX/raw/master/ShareX.UploadersLib/Helpers/CustomUploaderItem.cs
class Config {
    internal var Name: String? = null
    internal var DestinationType: String? = null
    internal var RequestType: String? = null
    internal var RequestURL: String? = null
    internal var FileFormName: String = ""
    internal var Headers: Map<String, String>? = null
    internal var Arguments: Map<String, String>? = null
    internal var RegexList: Array<String>? = null
    internal var ResponseType: String? = null
    internal var URL: String = ""

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

        if (URL.isEmpty())
            return response

        for (c in URL.toCharArray()) {
            if (c == '$') {
                insideQuery = !insideQuery

                if (!afterType)
                    query = queryType

                if (queryType.isNotEmpty()) {
                    val qt = String(queryType)
                    Log.d(TAG, "queryType $qt")
                    if (qt == "json")
                        doQuery = { JsonPath.read(response, it) }
                    else if (qt == "xml") // untested
                        doQuery = { XPathFactory.newInstance().newXPath().evaluate(it, InputSource(response.byteInputStream())) }
                    else if (qt == "random") // untested
                        doQuery = { it.split('|').getRandom() }
                    else
                        throw NotImplementedError("query type $qt not implemented")
                }

                if (query.isNotEmpty()) {
                    if (doQuery == null)
                        throw AssertionError("no query handler to execute $query")

                    Log.d(TAG, "query ${String(query)}")
                    rawResult.append(doQuery(String(query)))
                }

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
}
