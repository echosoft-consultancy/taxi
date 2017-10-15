package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.searchbox.core.Index
import spark.Request
import spark.Response
import java.time.LocalDateTime


val addPassengerCoordinate: (Request, Response) -> Map<String, Any> = { req, res ->
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
    val passengerItem = mapOf("email" to "test@example.com",
            "coordinates" to
                    mapOf("lat" to coordinates.lat, "lon" to coordinates.lon),
            "timestamp" to LocalDateTime.now().toString())
    jestClient.execute(Index.Builder(passengerItem).index("taxi").type("passenger").build())
    res.status(201)
    passengerItem
}