package no.nav.sf.logout

import io.prometheus.client.Gauge

data class WMetrics(
    val requestCount: Gauge = Gauge
        .build()
        .name("request_count")
        .help("request_count")
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
