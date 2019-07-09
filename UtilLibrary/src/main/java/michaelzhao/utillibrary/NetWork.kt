package michaelzhao.utillibrary

import android.content.Context
import michaelzhao.database.NetRequestType
import michaelzhao.database.NetWorkSqlCache
import michaelzhao.database.NetWorkRequest
import michaelzhao.database.DBConstUtil.getMilliSecond
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import michaelzhao.utillibrary.Util.NowDate
import java.util.concurrent.TimeUnit

class NetWork {

    companion object {
        //init the instance of Network before use it
        fun initNetWork(context: Context) {
            NetWorkSqlCache.initNetWorkCacheInstance(context)
        }

        var UNIT_TEST_MODE = false
        val Instance by lazy { NetWork() }
    }

    var ConnectTimeout = 10000L
    var ReadTimeout = 20000L
    var IsfollowRedirect = true
    var IsfollowSslRedirect = true
    var RequestType = NetRequestType.Day
    var UserAge =
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36"
    var Refer = ""

    private val mlistInterceptor = listOf<Interceptor>()

    private val mOkHttp: OkHttpClient
        get() {
            return OkHttpClient().newBuilder()
                .apply { mlistInterceptor.forEach { addInterceptor(it) } }
                .connectTimeout(ConnectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(ReadTimeout, TimeUnit.MILLISECONDS)
                .followRedirects(IsfollowRedirect)
                .followSslRedirects(IsfollowSslRedirect)
                .build()
        }

    private fun downLoadString(
        url: String, userAgent: String = UserAge,
        type: NetRequestType = RequestType
    ): String {
        var result = ""
        try {
            val real_url = url.replace(Regex("\\s+"), "+")
            val request = Request.Builder().url(real_url).addHeader("User-Agent", userAgent).apply {
                if (Refer.isNotBlank()) {
                    addHeader("referer", Refer)
                }
            }.build()
            val response = mOkHttp.newCall(request).execute()
            val body = response.body()
            if (body != null) {
                result = body.string()
                NetWorkSqlCache.Instance?.addNetRequest(url, result, type)
                println("From Net Response [${result.length}] : " + result.trim().removeCrlf().take(100) + "...")
            } else {
                println("From Net Error: Empty Response Body  $url")
            }
            response.close()
            return result
        } catch (ex: Exception) {
            println("From Net Error: $ex  $url")
        }
        return result
    }

    private fun isNeedNewRequest(url: String, type: NetRequestType): Pair<Boolean, String> {
        val data = NetWorkSqlCache.Instance?.findUrl(url)
        if (data != null) {
            val now = NowDate
            if (data != NetWorkRequest.NULL) {
                val duration = now.time - data.time.time
                val typeMS = getMilliSecond(type)
                if (duration <= typeMS) {

                    return Pair(false, data.content)
                }
            }
        }
        return Pair(true, "")
    }

    fun GetBytesFromNet(url: String, userAgent: String = UserAge): ByteArray {
        var response: Response? = null
        try {
            val real_url = url.replace(Regex("\\s+"), "+")
            val request = Request.Builder().url(real_url).addHeader("User-Agent", userAgent).build()
            response = mOkHttp.newCall(request).execute()
            val body = response.body()
            if (body != null) {
                return body.bytes()
            }
        } catch (ex: Exception) {
            println("From Net Error: $ex  $url")
        } finally {
            response?.close()
        }
        return ByteArray(0)
    }

    fun GetUrlContent(url: String, type: NetRequestType = NetRequestType.Day, userAgent: String = UserAge): String {
        if (UNIT_TEST_MODE) return GetUrlContentForTest(url)

        return Util.TimeElapse("Get Url Content") {
            println("Create Net Request: $url")
            val data = isNeedNewRequest(url, type)
            if (data.first) {
                val result = downLoadString(url, userAgent, type)
                if (result.isBlank() && data.second.isNotBlank()) {
                    println("From DataBase Response [${data.second}] : " + data.second.trim().removeCrlf().take(100) + "...")
                    return@TimeElapse data.second
                }
                return@TimeElapse result
            } else {
                println("From DataBase Response [${data.second}] : " + data.second.trim().removeCrlf().take(100) + "...")
                data.second
            }
        }
    }

    fun GetUrlContentForTest(url: String): String {
        var result = ""
        try {
            val real_url = url.replace(Regex("\\s+"), "+")
            val request = Request.Builder().url(real_url).build()
            val response = mOkHttp.newCall(request).execute()
            val body = response.body()
            if (body != null) {
                result = body.string()
                println("From Net Response [${result.length}] : " + result.trim().removeCrlf().take(100) + "...")
            } else {
                println("From Net Error: Empty Response Body  $url")
            }
            response.close()
            return result
        } catch (ex: Exception) {
            println("From Net Error: $ex  $url")
        }
        return result
    }

}