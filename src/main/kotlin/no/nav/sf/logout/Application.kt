package no.nav.sf.logout

import mu.KotlinLogging
import no.nav.sf.library.AVault
import no.nav.sf.library.AnEnvironment
import no.nav.sf.library.NAIS_DEFAULT_PORT

private val log = KotlinLogging.logger { }

object Application {

    val SFClientID = AVault.getSecretOrDefault("SFClientID", "")
    val SFClientSecret = AVault.getSecretOrDefault("SFClientSecret", "")
    val SFUsername = AVault.getSecretOrDefault("SFUsername", "")
    val SFPassword = AVault.getSecretOrDefault("SFPassword", "")
    val SFTokenHost = AnEnvironment.getEnvOrDefault("SF_TOKENHOST", "")
    val SFSecurityToken = AnEnvironment.getEnvOrDefault("SFSecurityToken", "")
    const val EV_httpsProxy = "HTTPS_PROXY"

    var accessToken: AccessToken = AccessToken("", "", "", "", "", "0", "") // Accesstoken at epoch

    fun start() {
        log.info { "Starting app" }
        naisAPIServer(NAIS_DEFAULT_PORT).start()
    }
}
