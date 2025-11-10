package no.nav.sf.logout

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.body.toBody

data class AccessToken(
    val access_token: String = "",
    val scope: String = "",
    val instance_url: String,
    val id: String = "",
    val token_type: String = "",
    val issued_at: String = "",
    val signature: String = "",
)

fun AccessToken.ageInMinutes(): Int = ((System.currentTimeMillis() - this.issued_at.toLong()) / 60000L).toInt()

fun doAccessTokenCall(callback: (Response) -> Unit) {
    val request =
        Request(
            Method.POST,
            Application.SFTokenHost,
        ).body(getBody()).header("Content-Type", "application/x-www-form-urlencoded")

    callback(client(request))
}

private fun getBody(): Body =
    listOf(
        "grant_type" to "password",
        "client_id" to Application.SFClientID,
        "client_secret" to Application.SFClientSecret,
        "username" to Application.SFUsername,
        "password" to Application.SFPassword + Application.SFSecurityToken,
    ).toBody()
