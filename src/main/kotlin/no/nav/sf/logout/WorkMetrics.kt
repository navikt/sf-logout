package no.nav.sf.logout

import io.prometheus.client.Counter

data class WMetrics(
    val requestCount: Counter = Metrics.registerCounter("requests"),
    val tokenRefreshCount: Counter = Metrics.registerCounter("token_refresh"),
    val issues: Counter = Metrics.registerLabelCounter("issues", "type"),
)

val workMetrics = WMetrics()
