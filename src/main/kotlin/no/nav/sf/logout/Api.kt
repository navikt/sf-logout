package no.nav.sf.logout

import io.prometheus.client.exporter.common.TextFormat
import mu.KotlinLogging
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
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
        workMetrics.requestCount.inc()

        if (request.query("sid") == null) Response(Status.BAD_REQUEST) else {
            val success = doLogoutCall(request.query("sid")!!)
            Response(if (success) Status.OK else Status.INTERNAL_SERVER_ERROR).body("Called logout endpoint, success: $success")
        }
    },
    "/isAlive" bind Method.GET to { Response(Status.OK) },
    "/isReady" bind Method.GET to { Response(Status.OK) },
    "/metrics" bind Method.GET to {
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
    }
)

private fun String.responseByContent(): Response =
    if (this.isNotEmpty()) Response(Status.OK).body(this) else Response(Status.NO_CONTENT)

fun naisAPIServer(port: Int): Http4kServer = naisAPI().asServer(Netty(port))

fun enableNAISAPIModified(port: Int = 8080, doSomething: () -> Unit): Boolean =
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
