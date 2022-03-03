package no.nav.sf.logout

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sf.library.AVault
import no.nav.sf.library.AnEnvironment
import no.nav.sf.library.PrestopHook
import no.nav.sf.library.ShutdownHook
import no.nav.sf.library.enableNAISAPI
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

        enableNAISAPI {
            log.info("Will wait half a minute with enabled NAIS API")
            conditionalWait(30000) // Wait half a minute

            // create(Dataprodukt.CHAT.getTableId(), Dataprodukt.CHAT.getSchema())

            // fetchAndSend(LocalDate.now().minusDays(1), Dataprodukt.CHAT)
            // fetchAndSend(LocalDate.now().minusDays(1), Dataprodukt.KNOWLEDGE)

            loop()
        }
        log.info { "App Finished!" }
    }

    fun runMigration() {
        val schema: Schema = Schema.of(
            Field.of("dato", StandardSQLTypeName.TIMESTAMP),
            Field.of("id", StandardSQLTypeName.STRING),
        )

        // createOrUpdate(schema)
    }

    // Alternative route saved as reference:
    // Timer starts every day at 05:00
    /*
    private fun initTimer() {
        val repeatedTask: TimerTask = object : TimerTask() {
            override fun run() {
                // initLoad()
            }
        }

        val timer = Timer("DailyTimer")
        // Every day
        val dailyPeriod = 1000L * 60L * 60L * 24L

        val date = Calendar.getInstance()
        date[Calendar.HOUR_OF_DAY] = 5
        date[Calendar.MINUTE] = 0
        date[Calendar.SECOND] = 0
        date[Calendar.MILLISECOND] = 0

        timer.scheduleAtFixedRate(repeatedTask, date.time, dailyPeriod)
    }

    private fun cancelTimer(timer: Timer) {
        timer.cancel()
    }

     */

    private tailrec fun loop() {

        log.info { " In loop " }

        val stop = ShutdownHook.isActive() || PrestopHook.isActive()
        when {
            stop -> Unit
            !stop -> {
                log.info { "Continue in loop... " }
                work()
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
