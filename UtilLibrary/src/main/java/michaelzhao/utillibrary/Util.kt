package michaelzhao.utillibrary

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.sizeDp
import com.mikepenz.iconics.typeface.IIcon
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object Util {

    val NowDate get() = Date(System.currentTimeMillis())

    fun Bitmap2Byte(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    fun GetRegexList(source: String, patStr: String, front: String = "", back: String = ""): List<String> {
        val pattern = Pattern.compile(patStr)
        val matcher = pattern.matcher(source)
        val listTxt = mutableListOf<String>()
        while (matcher.find()) {
            val thumb = matcher
                .group()
                .replace(front, "")
                .replace(back, "")
                .trim()
            listTxt.add(thumb)
        }
        return listTxt
    }

    fun Compress(bs: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = DeflaterOutputStream(bos)
        zos.write(bs)
        zos.close()
        return bos.toByteArray()
    }

    fun UnCompress(bs: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val zos = InflaterOutputStream(bos)
        zos.write(bs)
        zos.close()
        return bos.toByteArray()
    }

    fun <T> TimeElapse(str: String, action: () -> T): T {
        val start = System.nanoTime()
        val result = action()
        val elapse = System.nanoTime() - start
        println(str + " : ${elapse / 1000.0 / 1000.0} ms")
        return result
    }

    /**
     * 接受任意域名服务器
     */
    fun EnableHostnameVerifier() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (ex: Exception) {
            println("EnableHostnameVerifier: " + ex.message)
        }
    }


}