package no.nav.sf.logout

import com.google.gson.Gson
import mu.KotlinLogging
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.lang.IllegalStateException

private val log = KotlinLogging.logger {}

val client = OkHttp()

val gson = Gson()

fun doLogoutCallSF(
    instance_url: String,
    sid: String,
    token: String,
    callback: (Response) -> Unit,
) {
    val query = "/services/apexrest/idporten/logout"
    val uri = "$instance_url$query"
    log.info { "Will do post call: $uri, body $sid" }

    callback(
        client(
            Request(
                Method.POST,
                uri,
            ).header("Content-Type", "application/json").header("Authorization", "Bearer $token").body("{\"sid\":\"$sid\"}"),
        ),
    )
}

fun refreshAccessToken() {
    doAccessTokenCall {
        when (it.status) {
            Status.UNAUTHORIZED -> {
                log.error { "Access token call salesforce unauthorized" }
            }
            Status.OK -> {
                val result = gson.fromJson(it.bodyString(), AccessToken::class.java)
                if (result == null) {
                    workMetrics.issues.labels("access token empty response").inc()
                    throw IllegalStateException("Empty accesstoken returned")
                }
                Application.accessToken = result
                workMetrics.tokenRefreshCount.inc()
                log.info { "Access token refreshed" }
            }
            else -> {
                log.error { "Access token call salesforce NOK" }
                workMetrics.issues.labels("access token fetch NOK").inc()
            }
        }
    }
}

fun doLogoutCall(sid: String): Boolean {
    var confirmedSuccess = false
    try {
        if (Application.accessToken.ageInMinutes() > 10) refreshAccessToken()
        val instanceUrl = Application.accessToken.instance_url
        val token = Application.accessToken.access_token
        doLogoutCallSF(instanceUrl, sid, token) { response ->
            if (response.status == Status.OK) {
                log.info { "Got response status ${response.status} and body ${response.body}" }
                confirmedSuccess = true
            } else {
                log.error { "Got response status ${response.status} and body ${response.body}" }
                workMetrics.issues.labels("logout response ${response.status.code}").inc()
            }
        }
        return confirmedSuccess
    } catch (e: Exception) {
        log.error { "Exception catched: ${e.printStackTrace()}" }
        workMetrics.issues.labels("${e::class.simpleName}").inc()
        return false
    }
}
