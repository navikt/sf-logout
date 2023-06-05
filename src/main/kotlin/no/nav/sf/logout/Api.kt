package no.nav.sf.logout

import io.prometheus.client.exporter.common.TextFormat
import mu.KotlinLogging
import no.nav.sf.library.Metrics
import no.nav.sf.library.NAIS_DEFAULT_PORT
import no.nav.sf.library.NAIS_ISALIVE
import no.nav.sf.library.NAIS_ISREADY
import no.nav.sf.library.NAIS_METRICS
import no.nav.sf.library.NAIS_PRESTOP
import no.nav.sf.library.PrestopHook
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.io.StringWriter

private val log = KotlinLogging.logger { }

fun naisAPI(): HttpHandler = routes(
    // "/static" bind static(ResourceLoader.Classpath("/static")),
    "/logout" bind Method.GET to { request ->
        log.info { "Logout call" }
        log.info { "Request $request" }
        workMetrics.requestCount.inc()

        if (request.query("sid") == null) Response(Status.EXPECTATION_FAILED) else {
            val success = doLogoutCall(request.query("sid")!!)
            Response(if (success) Status.OK else Status.EXPECTATION_FAILED).body("Called logout endpoint, success: $success")
        }
    },
    "/c" bind Method.GET to { request ->
        val sidCookie = request.cookie("sid")

        if (sidCookie == null) {
            Response(Status.OK).body("No sid cookie found")
        } else {
            Response(Status.OK).body(sidCookie.value)
        }
    },
    NAIS_ISALIVE bind Method.GET to { Response(Status.OK) },
    NAIS_ISREADY bind Method.GET to { Response(Status.OK) },
    NAIS_METRICS bind Method.GET to {
        runCatching {
            StringWriter().let { str ->
                TextFormat.write004(str, Metrics.cRegistry.metricFamilySamples())
                str
            }.toString()
        }
            .onFailure {
                log.error { "/prometheus failed writing metrics - ${it.localizedMessage}" }
            }
            .getOrDefault("")
            .responseByContent()
    },
    NAIS_PRESTOP bind Method.GET to {
        PrestopHook.activate()
        log.info { "Received PreStopHook from NAIS" }
        Response(Status.OK)
    }
)

private fun String.responseByContent(): Response =
    if (this.isNotEmpty()) Response(Status.OK).body(this) else Response(Status.NO_CONTENT)

fun naisAPIServer(port: Int): Http4kServer = naisAPI().asServer(Netty(port))

fun enableNAISAPIModified(port: Int = NAIS_DEFAULT_PORT, doSomething: () -> Unit): Boolean =
    naisAPIServer(port).let { srv ->
        try {
            srv.start().use {
                log.info { "NAIS DSL is up and running at port $port" }
                runCatching(doSomething)
                    .onFailure {
                        log.error { "Failure during doSomething in enableNAISAPI - ${it.localizedMessage}" }
                    }
            }
            true
        } catch (e: Exception) {
            log.error { "Failure during enable/disable NAIS api for port $port - ${e.localizedMessage}" }
            false
        } finally {
            srv.close()
            log.info { "NAIS DSL is stopped at port $port" }
        }
    }
