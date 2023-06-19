package no.nav.sf.logout

import com.beust.klaxon.Klaxon
import mu.KotlinLogging
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.lang.IllegalStateException

private val log = KotlinLogging.logger {}

val client = ApacheClient.asHttpHandler()

fun doLogoutCallSF(instance_url: String, sid: String, token: String, callback: (Response) -> Unit) {
    val query = "/services/apexrest/idporten/logout"
    val uri = "$instance_url$query"
    log.info { "Will do post call: $uri, body $sid" }

    callback(client(Request(Method.POST, uri).header("Content-Type", "application/json").header("Authorization", "Bearer $token").body("{\"sid\":\"$sid\"}")))
}

fun refreshAccessToken() {
    doAccessTokenCall {
        when (it.status) {
            Status.UNAUTHORIZED -> {
                log.error { "Access token call salesforce unauthorized" }
            }
            Status.OK -> {
                val result = Klaxon().parse<AccessToken>(it.bodyString())
                if (result == null) {
                    workMetrics.issues.inc()
                    throw IllegalStateException("Empty accesstoken returned")
                }
                Application.accessToken = result
                workMetrics.tokenRefreshCount.inc()
                log.info { "Access token refreshed" }
            }
            else -> {
                log.error { "Access token call salesforce NOK" }
                workMetrics.issues.inc()
            }
        }
    }
}

fun doLogoutCall(sid: String): Boolean {
    var confirmedSuccess: Boolean = false
    try {
        if (Application.accessToken.ageInMinutes() > 10) refreshAccessToken()
        val instance_url = Application.accessToken.instance_url
        val token = Application.accessToken.access_token
        doLogoutCallSF(instance_url, sid, token) { response ->
            if (response.status == Status.OK) {
                log.info { "Got response status ${response.status} and body ${response.body}" }
                confirmedSuccess = true
            } else {
                log.error { "Got response status ${response.status} and body ${response.body}" }
                workMetrics.issues.inc()
            }
        }
        return confirmedSuccess
    } catch (e: Exception) {
        log.error { "Exception catched: ${e.printStackTrace()}" }
        workMetrics.issues.inc()
        return false
    }
}
