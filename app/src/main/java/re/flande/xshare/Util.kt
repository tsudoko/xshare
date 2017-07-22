package re.flande.xshare

import java.io.InputStream
import java.io.OutputStream

object Util {
    fun copy(in_: InputStream, out: OutputStream) {
        val buf = ByteArray(4096)
        var n = 0

        while(n != -1) {
            out.write(buf, 0, n)
            n = in_.read(buf)
        }
    }
}
