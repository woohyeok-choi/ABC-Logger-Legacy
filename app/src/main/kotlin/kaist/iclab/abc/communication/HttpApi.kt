package kaist.iclab.abc.communication

import android.content.Context
import android.telecom.Call
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kaist.iclab.abc.common.*
import kaist.iclab.abc.common.util.FormatUtils
import kaist.iclab.abc.common.util.NetworkUtils
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object HttpApi {
    private fun isRedirect(code: Int): Boolean {
        return when(code) {
            HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_SEE_OTHER -> true
            else -> false
        }
    }

    private fun getConnection(url: URL) : HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        conn.readTimeout = TimeUnit.MINUTES.toMillis(1).toInt()
        return conn
    }

    fun request(context: Context, url: String?) : Task<String> {
        return Tasks.call(Executors.newSingleThreadExecutor(), Callable {
            if(!FormatUtils.validateUrl(url)) throw InvalidUrlException()
            if(!NetworkUtils.isNetworkAvailable(context)) throw NoNetworkAvailableException()

            try {
                var redirectedUrl: URL? = null

                for (i in 0 until 10) {
                    redirectedUrl = redirectedUrl ?: URL(url)
                    val conn = getConnection(redirectedUrl)

                    if(isRedirect(conn.responseCode)) {
                        redirectedUrl = URL(conn.getHeaderField("Location"))
                        continue
                    } else if (conn.responseCode in 400 until 500) {
                        throw InvalidRequestException()
                    } else if (conn.responseCode >= 500) {
                        throw ServerUnavailableException()
                    } else {
                        return@Callable BufferedInputStream(conn.inputStream).reader(Charsets.UTF_8).readText()
                    }
                }
            } catch (e: SocketTimeoutException) {
                throw TimeoutException()
            } catch (e: Exception) {
                throw e
            }
            throw InvalidRequestException()
        })
    }
}