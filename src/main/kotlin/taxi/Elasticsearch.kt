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
import io.searchbox.client.JestResult
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.indices.CreateIndex
import io.searchbox.indices.mapping.PutMapping
import mu.KotlinLogging
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import spark.Response
import java.lang.Exception
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}


val taxiMapping =
"""
{
   "passenger":{
      "properties":{
         "email":{
            "type":"string",
            "store":"yes",
            "index": "not_analyzed"
         },
         "coordinates":{
            "type":"geo_point"
         },
         "timestamp":{
            "type":"date",
            "format":"date_hour_minute_second_millis"
         }
      }
   },
   "driver":{
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
            "format":"date_hour_minute_second_millis"
         }
      }
   },
   "rideRequest":{
      "properties":{
         "email":{
            "type":"string",
            "store":"yes"
         },
         "timestamp":{
            "type":"date",
            "format":"date_hour_minute_second_millis"
         }
      }
   },
   "ride":{
      "properties":{
         "passenger":{
            "type":"string",
            "store":"yes"
         },
         "driver":{
            "type":"string",
            "store":"yes"
         },
         "timestamp":{
            "type":"date",
            "format":"date_hour_minute_second_millis"
         }
      }
   }
}
"""

object elasticsearch : PropertyGroup() {
    val api by stringType
}

fun createElasticsearchClient() : JestClient {
    val factory = JestClientFactory()
    factory.setHttpClientConfig(HttpClientConfig
            .Builder(config[elasticsearch.api])
            .multiThreaded(true)
            .credentialsProvider(BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials("elastic", "changeme"))
            })
    .build())
    return factory.`object`
}

private fun retry(functionToRetry: () -> JestResult) {
    val retryer = RetryerBuilder.newBuilder<JestResult>()
            .retryIfResult {
                val statusCode = it?.responseCode.toString()
                if (statusCode.startsWith("40")) { logger.info("Failed with error: ${it?.errorMessage}")
                    false
                } else {
                    !statusCode.startsWith("20")
                }
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

fun setupSchema(jestClient: JestClient) {
    logger.info { "Creating taxi index with retry if failure " }
    retry({ jestClient.execute(CreateIndex.Builder("taxi").build()) })

    logger.info { "Creating taxi.passenger mapping with retry if failure" }
    val putMapping = PutMapping.Builder("taxi", "passenger", taxiMapping).build()
    retry({jestClient.execute(putMapping) })
}