package no.nav.sf.logout

import kotlinx.serialization.Serializable
import no.nav.sf.library.AnEnvironment
import org.http4k.client.ApacheClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.body.toBody

@Serializable
data class AccessToken(
    val access_token: String = "",
    val scope: String = "",
    val instance_url: String,
    val id: String = "",
    val token_type: String = "",
    val issued_at: String = "",
    val signature: String = ""
)

fun doAccessTokenCall(callback: (Response) -> Unit) {
    val client = ApacheClient.supportProxy(AnEnvironment.getEnvOrDefault(Bootstrap.EV_httpsProxy))

    val request = Request(
        Method.POST,
        Bootstrap.SFTokenHost
    ).body(getBody()).header("Content-Type", "application/x-www-form-urlencoded")

    callback(client(request))
}

private fun getBody(): Body {
    return listOf(
        "grant_type" to "password",
        "client_id" to Bootstrap.SFClientID,
        "client_secret" to Bootstrap.SFClientSecret,
        "username" to Bootstrap.SFUsername,
        "password" to Bootstrap.SFPassword + Bootstrap.SFSecurityToken
    ).toBody()
}
