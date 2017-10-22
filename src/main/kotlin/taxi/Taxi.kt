package taxi

import com.fasterxml.jackson.core.JsonParseException
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import mu.KotlinLogging
import spark.Request
import spark.Response
import spark.Spark.get
import spark.Spark.post
import spark.Spark.exception

private val logger = KotlinLogging.logger {}

val config = systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResource("defaults.properties")

val jestClient = createElasticsearchClient()

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY", "TRACE")
    setupSchema(jestClient)

    registerDriverApi()

    registerPassengerApi()

    registerHealthcheck()

    registerExceptionHandler()
}

private fun registerExceptionHandler() {
    exception(JsonParseException::class.java) { exception, req, res ->
        logger.error { "Caught JsonParseException" }
        res.status(400)
    }
}

private fun registerHealthcheck() {
    logger.info {
        """
        Registering routes:
    get("/healthcheck", healthcheck)
    """.trimIndent()
    }
    get("/healthcheck", healthcheck)
}

private fun registerPassengerApi() {
    logger.info {
        """
        Registering routes:
        post("/passenger/coordinate", addPassengerCoordinate)
        post("/passenger/ride", requestRide)
    """.trimIndent()
    }
    post("/passenger/coordinate", addPassengerCoordinate)
    post("/passenger/ride", requestRide)
}

private fun registerDriverApi() {
    logger.info {
        """
        Registering routes:
        post("/driver/coordinate", addDriverCoordinate)
        get("/driver/ride", getRideRequests)
        post("/driver/ride/offer", offerRide)
    """.trimIndent()
    }
    post("/driver/coordinate", addDriverCoordinate)
    get("/driver/ride", getRideRequests)
    post("/driver/ride/offer", offerRide)
}

val healthcheck = { request: Request, response: Response -> "ok" }