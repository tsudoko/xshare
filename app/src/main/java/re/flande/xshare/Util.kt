package re.flande.xshare

import android.view.View
import java.util.*

fun <T> List<T>.getRandom(): T {
    return get(Random().nextInt(size))
}

fun View.fade(show: Boolean, duration: Long) {
    animate()
            .setDuration(duration)
            .alpha(if(show) 1F else 0F)
            .withEndAction { visibility = if(show) View.VISIBLE else View.GONE }
}

fun View.fadeIn(duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()) {
    fade(true, duration)
}

fun View.fadeOut(duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()) {
    fade(false, duration)
}