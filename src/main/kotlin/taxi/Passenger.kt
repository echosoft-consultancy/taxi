package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.searchbox.core.Index
import spark.Request
import spark.Response
import java.time.LocalDateTime

data class PassengerCoordinate(val email: String, val timestamp : LocalDateTime, val coordinates: Coordinate)


val addPassengerCoordinate: (Request, Response) -> PassengerCoordinate = { req, res ->
    // Parse coordinates from json
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())

    // Add new passenger coordinates to elasticsearch passenger mapping
    val passengerCoordinate = PassengerCoordinate(email = "test@example.com",
            timestamp = LocalDateTime.now(),
            coordinates = coordinates)
    jestClient.execute(Index.Builder(passengerCoordinate).index("taxi").type("passenger").build())
    res.status(201)
    passengerCoordinate
}