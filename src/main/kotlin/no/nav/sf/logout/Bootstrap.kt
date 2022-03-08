package no.nav.sf.logout

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sf.library.AVault
import no.nav.sf.library.AnEnvironment
import no.nav.sf.library.PrestopHook
import no.nav.sf.library.ShutdownHook
import java.time.LocalTime

private const val EV_bootstrapWaitTime = "MS_BETWEEN_WORK" // default to 10 minutes
private val bootstrapWaitTime = AnEnvironment.getEnvOrDefault(EV_bootstrapWaitTime, "60000").toLong()

private val resetRangeStart = LocalTime.parse("00:00:01")
private val resetRangeStop = LocalTime.parse("03:59:00")

var hasPostedToday = false

private val log = KotlinLogging.logger { }

object Bootstrap {

    val AUDITLOG = KotlinLogging.logger("AuditLogger")

    val SFClientID = AVault.getSecretOrDefault("SFClientID", "")
    val SFClientSecret = AVault.getSecretOrDefault("SFClientSecret", "")
    val SFUsername = AVault.getSecretOrDefault("SFUsername", "")
    val SFPassword = AVault.getSecretOrDefault("SFPassword", "")
    val SFTokenHost = AnEnvironment.getEnvOrDefault("SF_TOKENHOST", "")
    val SFSecurityToken = AnEnvironment.getEnvOrDefault("SFSecurityToken", "")
    const val EV_httpsProxy = "HTTPS_PROXY"

    val projectId = System.getenv("GCP_TEAM_PROJECT_ID")

    fun start() {
        log.info { "Starting app" }
        /*
        enableNAISAPI {
            // manualTriggeredDayLog("2021-10-08")
            // fetchAndLogSpecial()
            loop()
        }
        private fun conditionalWait(ms: Long = bootstrapWaitTime)
         */

        enableNAISAPIModified {
            log.info("Will wait half a minute with enabled NAIS API")
            conditionalWait(30000) // Wait half a minute

            loop()
        }
        log.info { "App Finished!" }
    }

    private tailrec fun loop() {

        // log.info { " In loop " }

        val stop = ShutdownHook.isActive() || PrestopHook.isActive()
        when {
            stop -> Unit
            !stop -> {
                // log.info { "Continue in loop... " }
                // work()
                /*
                if (hasPostedToday) {
                    if (LocalTime.now().inResetRange()) {
                        log.warn { "Giving up on previous day - resetting posted flag" }
                        hasPostedToday = false
                    } else {
                        // log.info { "Has posted logs today - will sleep 30 minutes." }
                    }
                } else {
                    if (LocalTime.now().inActiveRange()) {
                        work()
                    } else {
                        log.info { "Waiting for active range (later then ${resetRangeStop.format(DateTimeFormatter.ISO_DATE)}) - will sleep 30 minutes." }
                    }
                }
                conditionalWait(1800000) // Half an hour

                */
                loop()
            }
        }
    }

    private fun conditionalWait(ms: Long = bootstrapWaitTime) =
        runBlocking {

            val cr = launch {
                runCatching { delay(ms) }
                    .onSuccess { /*log.info { "waiting completed" }*/ }
                    .onFailure { /*log.info { "waiting interrupted" }*/ }
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

    fun LocalTime.inResetRange(): Boolean {
        return this.isAfter(resetRangeStart) && this.isBefore(resetRangeStop)
    }

    fun LocalTime.inActiveRange(): Boolean {
        return this.isAfter(resetRangeStop)
    }
}
