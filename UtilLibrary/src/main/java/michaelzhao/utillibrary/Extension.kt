package michaelzhao.utillibrary

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.sizeDp
import com.mikepenz.iconics.typeface.IIcon
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.anko.doAsync
import java.net.URLDecoder
import java.net.URLEncoder

//region String Extension

fun String.fromURL(): String {
    return URLDecoder.decode(this, "utf-8")
}

fun String.toURL(): String {
    return URLEncoder.encode(this, "utf-8")
}

fun String.fromBase64(): String {
    return String(Base64.decode(this.replace("_", "/").toByteArray(), Base64.DEFAULT))
}

fun String.toBase64(): String {
    return String(Base64.encode(this.toByteArray(), Base64.DEFAULT))
        .replace("/", "_")
        .replace("\n", "")
        .replace("\r", "")
}

fun String.toUnicode(): String {
    return StringEscapeUtils.unescapeJava(this)
}

fun String.fromUnicode(): String {
    return StringEscapeUtils.escapeJava(this)
}

fun String.removeCrlf(): String {
    return this.replace("\n", "").replace("\r", "")
}

//endregion

//region Thread Extension

fun Any.runOnUIThread(run: () -> Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
        run()
    } else {
        ContextHelper.handler.post(run)
    }
}

fun Any.uiThread(run: () -> Unit) {
    runOnUIThread(run)
}

fun Any.uiThread(delay: Long, run: () -> Unit) {
    doAsync {
        Thread.sleep(delay)
        ContextHelper.handler.post(run)
    }
}

private object ContextHelper {
    val handler = Handler(Looper.getMainLooper())
    val mainThread: Thread = Looper.getMainLooper().thread
}

//endregion

//region Android View Extension

fun ViewGroup.enable(): View {
    uiThread { isEnabled = true }
    return this
}

fun ViewGroup.disEnable(): View {
    uiThread { isEnabled = false }
    return this
}

fun ViewGroup.show(): ViewGroup {
    uiThread { visibility = VISIBLE }
    return this
}

fun ViewGroup.hide(): ViewGroup {
    uiThread { visibility = GONE }
    return this
}

fun ViewGroup.onClick(run: () -> Unit) {
    setOnClickListener { run.invoke() }
}

fun View.enable() {
    uiThread { isEnabled = true }
}

fun View.disEnable() {
    uiThread { isEnabled = false }
}

fun MenuItem.onItemClick(action: () -> Unit): MenuItem {
    setOnMenuItemClickListener {
        action.invoke()
        return@setOnMenuItemClickListener true
    }
    return this
}

fun <T> MutableList<out T>.removeLast() {
    if (size > 1) removeAt(size - 1)
}

fun View.onClick(run: () -> Unit) {
    setOnClickListener { run.invoke() }
}

fun View.show(): View {
    uiThread { visibility = VISIBLE }
    return this
}

fun View.hide(): View {
    uiThread { visibility = GONE }
    return this
}

fun View.setHeight(height: Int) {
    val layoutParams = this.layoutParams
    layoutParams.height = height
    this.layoutParams = layoutParams
}

fun View.setWidth(height: Int) {
    val layoutParams = this.layoutParams
    layoutParams.width = height
    this.layoutParams = layoutParams
}

fun Context.inflate(id: Int, viewGroup: ViewGroup): View {
    return LayoutInflater.from(this).inflate(id, viewGroup, false)
}

fun Context.getHeightInPx(): Float {
    return this.resources.displayMetrics.heightPixels.toFloat()
}

fun Context.getWidthInPx(): Float {
    return this.resources.displayMetrics.widthPixels.toFloat()
}

fun Context.getColorValue(color: Int): Int {
    return ContextCompat.getColor(this, color)
}

fun Context.toast(str: String, duration: Int) {
    Toast.makeText(this, str, duration).show()
}

fun Context.getDrawableIcon(icon: IIcon, size: Int = 18): IconicsDrawable {
    return IconicsDrawable(this).icon(icon).color(IconicsColor.parse("white")).sizeDp(size)
}

fun Activity.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = this.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = this.resources.getDimensionPixelSize(resourceId)
    }
    return result
}

//endregion

