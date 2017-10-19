package taxi

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding
import spark.Spark.post


val config = systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResource("defaults.properties")

val jestClient = createElasticsearchClient()

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY", "TRACE")
    setupSchema(jestClient)

    // Register rest endpoints
    post("/driver/coordinate", addDriverCoordinate)
    post("/passenger/coordinate", addPassengerCoordinate)
}