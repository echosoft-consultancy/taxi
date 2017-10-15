package taxi

import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.intType
import com.natpryce.konfig.stringType
import io.searchbox.client.JestClient
import io.searchbox.client.JestClientFactory
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.indices.CreateIndex
import io.searchbox.indices.mapping.PutMapping
import mu.KotlinLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider


private val logger = KotlinLogging.logger {}

val passengerMapping =
""""{"passenger" : {
    |"properties" :{
            |"email" : |{
                |"type" : "string",
                |"store" : "yes"
            |}, {
            |"coordinates": {
                |"type": "geo_point"
            |}, {
            |"timestamp": {
                |"type": "date",
                |"format": "basic_date_time"
            |},
        |},
    |}
|}""".trimMargin()

object elasticsearchConfig : PropertyGroup() {
    val port by intType
    val host by stringType
}

fun client() : JestClient {
    val factory = JestClientFactory()
    factory.setHttpClientConfig(HttpClientConfig
            .Builder("http://${config[elasticsearchConfig.host]}:${config[elasticsearchConfig.port]}")
            .multiThreaded(true)
            .credentialsProvider(BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials("elastic", "changeme"))
            })
    .build())
    return factory.`object`
}

fun setupSchema(jestClient: JestClient) {
    logger.info { "Creating taxi index" }
    jestClient.execute(CreateIndex.Builder("taxi").build())
    val putMapping = PutMapping.Builder("taxi", "passenger", passengerMapping).build()
    logger.info { "Creating taxi.passenger mapping" }
    jestClient.execute(putMapping)
}