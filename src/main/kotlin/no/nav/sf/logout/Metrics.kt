package no.nav.sf.logout

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging

object Metrics {
    private val log = KotlinLogging.logger { }
    val cRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

    fun registerGauge(name: String): Gauge {
        return Gauge.build().name(name).help(name).register()
    }
    fun registerLabelGauge(name: String, label: String): Gauge {
        return Gauge.build().name(name).help(name).labelNames(label).register()
    }
    init {
        DefaultExports.initialize()
        log.info { "Prometheus metrics are ready" }
    }
}

data class WMetrics(
    val requestCount: Gauge = Gauge
        .build()
        .name("request_count")
        .help("request_count")
        .register(),
    val tokenRefreshCount: Gauge = Gauge
        .build()
        .name("token_refresh_count")
        .help("token_refresh_count")
        .register(),
    val issues: Gauge = Gauge
        .build()
        .name("issues")
        .help("issues")
        .register()
) {
    fun clearAll() {
        requestCount.clear()
        issues.clear()
    }
}

val workMetrics = WMetrics()
