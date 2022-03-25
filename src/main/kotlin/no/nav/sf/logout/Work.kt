package no.nav.sf.logout

import com.beust.klaxon.Klaxon
import mu.KotlinLogging
import no.nav.sf.library.AnEnvironment
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.io.File

private val log = KotlinLogging.logger {}

sealed class ExitReason {
    object NoSFClient : ExitReason()
    object NoKafkaClient : ExitReason()
    object NoEvents : ExitReason()
    object Work : ExitReason()
}

fun doLogoutCallSF(instance_url: String, sid: String, token: String, callback: (Response) -> Unit) {
    val client = ApacheClient.supportProxy(AnEnvironment.getEnvOrDefault(Bootstrap.EV_httpsProxy))
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
                Bootstrap.accessToken = Klaxon().parse<AccessToken>(it.bodyString())
                File("/tmp/at").writeText("access_token: ${Bootstrap.accessToken!!.access_token} \nissued at: ${Bootstrap.accessToken!!.issued_at} Age in minutes: ${Bootstrap.accessToken?.ageInMinutes()}")
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
    var refreshedToken = false
    var confirmedSuccess: Boolean = false
    try {
        refreshAccessToken()
        val instance_url = Bootstrap.accessToken!!.instance_url
        val token = Bootstrap.accessToken!!.access_token

        /*
            doLogoutCallSF(instance_url, sid, token) { response ->
                if (response.status == Status.OK) {
                    confirmedSuccess = true
                } else {
                    log.error { "Got response status ${response.status} and body ${response.body}" }
                    workMetrics.issues.inc()
                }
            }

         */
        confirmedSuccess = true
    } catch (e: Exception) {
        log.error { "Exception catched:  ${e.printStackTrace()}" }
        workMetrics.issues.inc()
        return false
    }
    return confirmedSuccess
}

fun work(): ExitReason {
    log.info { "Work session starting" }
    workMetrics.clearAll()
    // val successChat = doLogoutCall()
    log.info { "Work session finished" }

    return ExitReason.Work
}
