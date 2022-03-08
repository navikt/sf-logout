package no.nav.sf.logout

import com.beust.klaxon.Klaxon
import mu.KotlinLogging
import no.nav.sf.library.AnEnvironment
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

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
/*
fun doContinuationGetCall(instance_url: String, nextRecordsUrl: String, token: String, callback: (Response) -> Unit) {
    val client = ApacheClient.supportProxy(AnEnvironment.getEnvOrDefault(Bootstrap.EV_httpsProxy))
    val uri = "$instance_url$nextRecordsUrl"
    log.info { "Will do continuation get call: $uri" }
    callback(client(Request(Method.GET, uri).header("Authorization", "Bearer $token")))
}

// Returns done and nextRecordsUrl
fun Response.parseResponseAndSendBigQuery(dataprodukt: Dataprodukt): Pair<Boolean, String> {
    log.info { "Call for $dataprodukt - Response bodyString:\n ${this.bodyString()}" }
    var nextRecordsUrl = ""
    var done = when (dataprodukt) {
        Dataprodukt.CHAT -> {
            Klaxon().parse<ResponseChatQuery>(this.bodyString())?.let {
                log.info { "Parsed CHAT" } // TODO Temp test
                it.records?.let { records ->
                    if (records.isNotEmpty()) {
                        for (record in records) {
                            log.info { "$dataprodukt record start: ${record.StartTime}" }
                        }
                        if (insertChat(records)) {
                            workMetrics.postedChat.inc(records.size.toDouble())
                        }
                    }
                }
                if (!it.done) nextRecordsUrl = it.nextRecordsUrl!!
                it.done
            }
        }
        Dataprodukt.KNOWLEDGE -> {
            Klaxon().parse<ResponseKnowledgeQuery>(this.bodyString())?.let {
                it.records?.let { records ->
                    if (records.isNotEmpty()) {
                        for (record in records) {
                            log.info { "$dataprodukt record: $record" }
                        }
                        if (insertKnowledge(records)) {
                            workMetrics.postedKnowledge.inc(records.size.toDouble())
                        }
                    }
                }
                if (!it.done) nextRecordsUrl = it.nextRecordsUrl!!
                it.done
            }
        }
    }
    if (done == null) {
        throw IllegalStateException("Done should never be null")
    }
    return Pair(done, nextRecordsUrl)
}

 */

internal fun doLogoutCall(sid: String): Boolean {
    var confirmedSuccess: Boolean = false
    try {
        doAccessTokenCall {
            when (it.status) {
                Status.UNAUTHORIZED -> {
                    log.error { "Access token call salesforce unauthorized" }
                }
                Status.OK -> {
                    // log.info { "We should be authorized" }
                    val accessToken = Klaxon().parse<AccessToken>(it.bodyString())

                    if (accessToken != null) {
                        // log.info { "We should be authorized with accesstoken" }
                        val instance_url = accessToken.instance_url
                        val token = accessToken.access_token
                        log.info { "INVESTIGATE -  got ourselves a token!" }

                        doLogoutCallSF(instance_url, sid, token) { response ->
                            log.info { "INVESTIGATE - Got response status ${response.status} and body ${response.body}" }
                            if (response.status == Status.OK) {
                                confirmedSuccess = true
                            }
                        }
                    }
                }
                else -> {
                    log.error { "Access token call salesforce NOK" }
                    workMetrics.issues.inc()
                }
            }
        }
    } catch (e: Exception) {
        log.error { "Exception catched: ${e.printStackTrace()}" }
        workMetrics.issues.inc()
        return false
    }
    return confirmedSuccess
}

internal fun work(): ExitReason {
    log.info { "Work session starting" }
    workMetrics.clearAll()
    // val successChat = doLogoutCall()
    log.info { "Work session finished" }

    return ExitReason.Work
}
