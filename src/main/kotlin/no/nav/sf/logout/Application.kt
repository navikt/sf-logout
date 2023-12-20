package no.nav.sf.logout

import mu.KotlinLogging

private val log = KotlinLogging.logger { }

object Application {

    val SFClientID = System.getenv("SFClientID")
    val SFClientSecret = System.getenv("SFClientSecret")
    val SFUsername = System.getenv("SFUsername")
    val SFPassword = System.getenv("SFPassword")
    val SFTokenHost = System.getenv("SF_TOKENHOST")
    val SFSecurityToken = System.getenv("SFSecurityToken")
    const val EV_httpsProxy = "HTTPS_PROXY"

    var accessToken: AccessToken = AccessToken("", "", "", "", "", "0", "") // Accesstoken at epoch

    fun start() {
        log.info { "Starting app" }
        naisAPIServer(8080).start()
    }
}
