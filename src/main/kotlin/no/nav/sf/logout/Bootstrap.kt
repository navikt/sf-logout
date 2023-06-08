package no.nav.sf.logout

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sf.library.AVault
import no.nav.sf.library.AnEnvironment
import no.nav.sf.library.PrestopHook
import no.nav.sf.library.ShutdownHook

private const val EV_bootstrapWaitTime = "MS_BETWEEN_WORK" // default to 10 minutes
private val bootstrapWaitTime = AnEnvironment.getEnvOrDefault(EV_bootstrapWaitTime, "60000").toLong()

private val log = KotlinLogging.logger { }

object Bootstrap {

    val SFClientID = AVault.getSecretOrDefault("SFClientID", "")
    val SFClientSecret = AVault.getSecretOrDefault("SFClientSecret", "")
    val SFUsername = AVault.getSecretOrDefault("SFUsername", "")
    val SFPassword = AVault.getSecretOrDefault("SFPassword", "")
    val SFTokenHost = AnEnvironment.getEnvOrDefault("SF_TOKENHOST", "")
    val SFSecurityToken = AnEnvironment.getEnvOrDefault("SFSecurityToken", "")
    const val EV_httpsProxy = "HTTPS_PROXY"

    val projectId = System.getenv("GCP_TEAM_PROJECT_ID")

    var accessToken: AccessToken = AccessToken("", "", "", "", "", "0", "") // Accesstoken at epoch

    fun start() {
        log.info { "Starting app" }

        enableNAISAPIModified {
            log.info("Will wait half a minute with enabled NAIS API")
            conditionalWait(30000) // Wait half a minute
            refreshAccessToken()

            loop()
        }
        log.info { "App Finished!" }
    }

    private tailrec fun loop() {

        val stop = ShutdownHook.isActive() || PrestopHook.isActive()
        when {
            stop -> Unit
            !stop -> {
                conditionalWait()
                loop()
            }
        }
    }

    private fun conditionalWait(ms: Long = bootstrapWaitTime) =
        runBlocking {

            val cr = launch {
                runCatching { delay(ms) }
                    .onSuccess { }
                    .onFailure { }
            }

            tailrec suspend fun loop(): Unit = when {
                cr.isCompleted -> Unit
                ShutdownHook.isActive() || PrestopHook.isActive() -> cr.cancel()
                else -> {
                    delay(250L)
                    loop()
                }
            }

            loop()
            cr.join()
        }
}
