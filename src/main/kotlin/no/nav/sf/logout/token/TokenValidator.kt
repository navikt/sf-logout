package no.nav.sf.logout.token

import mu.KotlinLogging
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler
import org.http4k.core.Request
import java.io.File
import java.net.URL
import java.util.Optional

object TokenValidator {
    private val idportenAlias = "idporten"
    private val idportenUrl = System.getenv("IDPORTEN_WELL_KNOWN_URL")
    val idportenAudience = System.getenv("IDPORTEN_AUDIENCE").split(',')

    private val log = KotlinLogging.logger { }

    private val callerList: MutableMap<String, Int> = mutableMapOf()

    private val multiIssuerConfiguration = MultiIssuerConfiguration(
        mapOf(
            idportenAlias to IssuerProperties(URL(idportenUrl), idportenAudience)
        )
    )

    private val jwtTokenValidationHandler = JwtTokenValidationHandler(multiIssuerConfiguration)

    fun containsValidToken(request: Request): Boolean {
        val firstValidToken = jwtTokenValidationHandler.getValidatedTokens(request.toNavRequest()).firstValidToken
        return firstValidToken.isPresent
    }

    var latestValidationTime = 0L

    fun firstValidToken(request: Request): Optional<JwtToken> {
        lateinit var result: Optional<JwtToken>
        result = jwtTokenValidationHandler.getValidatedTokens(request.toNavRequest()).firstValidToken
        if (!result.isPresent) {
            File("/tmp/novalidtoken").writeText(request.toMessage())
        }
        return result
    }

    private fun Request.toNavRequest(): HttpRequest {
        val req = this
        return object : HttpRequest {
            override fun getHeader(headerName: String): String {
                return req.header(headerName) ?: ""
            }
            override fun getCookies(): Array<HttpRequest.NameValue> {
                return arrayOf()
            }
        }
    }
}
