package re.flande.xshare

import android.app.Application
import android.os.StrictMode

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        // ignore fileUriExposedException
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
    }
}