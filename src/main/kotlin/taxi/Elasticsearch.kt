package taxi

import com.github.rholder.retry.RetryerBuilder
import com.github.rholder.retry.StopStrategies
import com.github.rholder.retry.WaitStrategies
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
import java.lang.Exception
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}


val passengerMapping =
"""
{
   "passenger":{
      "properties":{
         "email":{
            "type":"string",
            "store":"yes"
         },
         "coordinates":{
            "type":"geo_point"
         },
         "timestamp":{
            "type":"date",
            "format":"basic_date_time"
         }
      }
   }
}
"""

object elasticsearch : PropertyGroup() {
    val port by intType
    val host by stringType
}

fun createElasticsearchClient() : JestClient {
    val factory = JestClientFactory()
    factory.setHttpClientConfig(HttpClientConfig
            .Builder("http://${config[elasticsearch.host]}:${config[elasticsearch.port]}")
            .multiThreaded(true)
            .credentialsProvider(BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials("elastic", "changeme"))
            })
    .build())
    return factory.`object`
}

private fun retry(functionToRetry: () -> Int) {
    val retryer = RetryerBuilder.newBuilder<Int>()
            .retryIfResult {
                logger.info { "HTTP status of elasticsearch operation $it" }
                val successfulOrAlreadyCreated = !it.toString().startsWith("20") and !it.toString().startsWith("40")
                successfulOrAlreadyCreated
            }
            .retryIfException {
                logger.info { "Exception thrown $it" }
                it is Exception
            }
            .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterDelay(30, TimeUnit.SECONDS))
            .build()
    retryer.call(functionToRetry)
}

fun setupSchema(jestClient: JestClient) {logger.info { "Creating taxi index with retry if failure " }
    logger.info { "Creating taxi index with retry if failure " }
    retry({ jestClient.execute(CreateIndex.Builder("taxi").build()).responseCode })

    logger.info { "Creating taxi.passenger mapping with retry if failure" }
    val putMapping = PutMapping.Builder("taxi", "passenger", passengerMapping).build()
    retry({jestClient.execute(putMapping).responseCode})
}