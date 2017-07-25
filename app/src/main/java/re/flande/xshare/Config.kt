package re.flande.xshare

import android.util.Log
import com.jayway.jsonpath.JsonPath

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
     * type ::= "json"
     * actual-query = [^$]*
     */
    fun prepareUrl(response: String): String {
        val rawResult = StringBuilder()
        var insideDollar = false
        var afterColon = false
        var query = charArrayOf()
        var queryType = charArrayOf()
        var doQuery: ((String) -> String)? = null

        for(c in URL.toCharArray()) {
            if(c == '$') {
                // FIXME (?): would most likely break on $json:asd['$']$
                insideDollar = !insideDollar

                if(!afterColon)
                    query = queryType

                if(queryType.isNotEmpty()) {
                    val qt = String(queryType)
                    Log.d(TAG, "queryType $qt")
                    if(qt == "json")
                        doQuery = { JsonPath.read(response, it) }
                    else
                        throw NotImplementedError("query type $qt not implemented")
                }

                if(query.isNotEmpty()) {
                    if(doQuery == null)
                        throw AssertionError("no query handler to execute $query")

                    Log.d(TAG, "query ${String(query)}")
                    rawResult.append(doQuery(String(query)))
                }

                query = charArrayOf()
                queryType = charArrayOf()
                doQuery = null
                afterColon = false

                continue
            }

            if(insideDollar) {
                if(c == ':' && !afterColon) {
                    afterColon = true
                    continue
                }

                if(afterColon)
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
