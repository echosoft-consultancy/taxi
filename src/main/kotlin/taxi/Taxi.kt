package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Spark.post

fun main(args: Array<String>) {
    post("/driver/coordinate") { req, res ->
        val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
        jacksonObjectMapper().writeValueAsString(coordinates)
    }
    post("/passenger/coordinate") { req, res ->
        val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
        jacksonObjectMapper().writeValueAsString(coordinates)
    }
}