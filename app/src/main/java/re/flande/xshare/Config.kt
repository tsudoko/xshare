package re.flande.xshare

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

    fun prepareUrl(response: String): String {
        if(URL.startsWith("${'$'}json:") && URL.endsWith("${'$'}")) {
            val query = "${'$'}." + URL.removePrefix("${'$'}json:").removeSuffix("${'$'}")
            return JsonPath.read(response, query)
        } else if(URL.startsWith('$')) {
            throw NotImplementedError()
        } else {
            return response
        }
    }
}
