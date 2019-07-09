package michaelzhao.database

import android.content.Context
import android.database.Cursor
import michaelzhao.database.DBConstUtil.CONTENT
import michaelzhao.database.DBConstUtil.TIME
import michaelzhao.database.DBConstUtil.EXPIRE
import michaelzhao.database.DBConstUtil.URL
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import michaelzhao.database.DBConstUtil.FORMATTER
import michaelzhao.utillibrary.Util
import michaelzhao.utillibrary.Util.NowDate

internal class NetWorkSqlCache(context: Context) : SQLiteOpenHelper(context, DATABASE_FILENAME, null, 1) {

    companion object {
        private const val DATABASE_FILENAME = "UtilLibraryNetWorkDataCache.db"
        private const val TABLE_REQUEST = "NetworkRequestCache"
        private const val CREATE_NETWORK_DATABASE_CACHE: String = "create table $TABLE_REQUEST (" +
                "id integer primary key autoincrement, " +
                "$URL text, " +
                "$CONTENT text, " +
                "$TIME text, " +
                "$EXPIRE text )"

        internal fun initNetWorkCacheInstance(context: Context) {
            _instance = NetWorkSqlCache(context)
        }

        private var _instance: NetWorkSqlCache? = null
        val Instance by lazy { _instance }
    }

    private val mLock = Any()

    override fun onCreate(db: SQLiteDatabase?) {
        synchronized(mLock) {
            try {
                db?.execSQL(CREATE_NETWORK_DATABASE_CACHE)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun addNetRequest(request: NetWorkRequest) {
        synchronized(mLock) {
            val db = writableDatabase
            try {
                if (existUrl(request.url)) {
                    db.update(TABLE_REQUEST, request.toSqlData(), "$URL=?", arrayOf(request.url))
                    println("DateBase[$TABLE_REQUEST] : update ${request.url} into cache michaelzhao.database")
                } else {
                    db.insert(TABLE_REQUEST, null, request.toSqlData())
                    println("DateBase[$TABLE_REQUEST] : insert ${request.url} into cache michaelzhao.database")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                db.close()
            }
        }
    }

    fun addNetRequest(url: String, content: String, type: NetRequestType = NetRequestType.Day) {
        addNetRequest(NetWorkRequest(url, content, NowDate, type))
    }

    private fun existUrl(url: String): Boolean {
        synchronized(mLock) {
            val db = writableDatabase
            return try {
                val cursor = db.query(TABLE_REQUEST, arrayOf(URL), "$URL=?", arrayOf(url), null, null, null)
                val cnt = cursor.count
                cursor.close()
                cnt > 0
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    fun findUrl(url: String): NetWorkRequest {
        synchronized(mLock) {
            val db = writableDatabase
            return try {
                val cursor = db.query(
                    TABLE_REQUEST,
                    arrayOf(URL, CONTENT, TIME, EXPIRE),
                    "$URL=?",
                    arrayOf(url),
                    null,
                    null,
                    null
                )
                if (cursor.moveToFirst()) {
                    val request = getValueFromCursor(cursor)
                    cursor.close()
                    request
                } else {
                    NetWorkRequest.NULL
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                NetWorkRequest.NULL
            } finally {
                db.close()
            }
        }
    }

    private fun getValueFromCursor(cursor: Cursor): NetWorkRequest {
        return try {
            NetWorkRequest(
                cursor.getString(0),
                String(Util.UnCompress(cursor.getBlob(1))),
                FORMATTER.parse(cursor.getString(2)),
                NetRequestType.valueOf(cursor.getString(3))
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            NetWorkRequest.NULL
        }
    }

}