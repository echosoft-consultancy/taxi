package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.searchbox.core.Index
import mu.KotlinLogging
import spark.Request
import spark.Response

private val logger = KotlinLogging.logger {}

val noBody = ""

val addPassengerCoordinate: (Request, Response) -> String = { req, res ->
    // Parse coordinates from json
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
    logger.info { "Adding passenger coordinates: $coordinates" }

    storePassengerCoordinates(UserCoordinate(email = "test@example.com",
            coordinates = coordinates))
    res.status(201)
    noBody
}

val requestRide: (Request, Response) -> String = { req, res ->
    // Parse coordinates from json
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
    logger.info { "Requesting ride with coordinates: $coordinates" }

    storePassengerCoordinates(UserCoordinate(email = "test@example.com",
            coordinates = coordinates))
    storeRide(RideRequest(email = "test@example.com"))
    res.status(201)
    noBody
}

private fun storePassengerCoordinates(passengerCoordinate: UserCoordinate) {
    // Add new passenger coordinates to elasticsearch passenger mapping
    jestClient.execute(Index.Builder(passengerCoordinate).index("taxi").type("passenger").build())
}

private fun storeRide(rideRequest: RideRequest) {
    // Add new rideRequest to elasticsearch ride mapping
    jestClient.execute(Index.Builder(rideRequest).index("taxi").type("rideRequest").build())
}