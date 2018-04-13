package re.flande.xshare

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.View
import java.util.*

fun <T> List<T>.getRandom(): T = get(Random().nextInt(size))

fun getFatalDialogBuilder(context: Activity): AlertDialog.Builder =
        AlertDialog.Builder(context).setOnDismissListener { context.finishAffinity() }

fun Uri.getFilename(context: Context): String {
    var name: String? = null
    context.contentResolver.query(this, null, null, null, null).use { cursor ->
        if (cursor?.moveToFirst() ?: return@use) {
            name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }

    return name ?: lastPathSegment ?: throw Exception("don't know how to get file name from $this")
}

fun View.fade(show: Boolean, duration: Long) {
    animate()
            .setDuration(duration)
            .alpha(if (show) 1F else 0F)
            .withEndAction { visibility = if (show) View.VISIBLE else View.GONE }
}

fun View.fadeIn(duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()) =
        fade(true, duration)

fun View.fadeOut(duration: Long = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()) =
        fade(false, duration)

fun Notification.Builder.setContentInfoN(text: CharSequence?): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            setSubText(text)
        else
            setContentInfo(text)