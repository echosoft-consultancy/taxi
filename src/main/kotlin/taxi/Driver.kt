package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import spark.Request
import spark.Response


val addDriverCoordinate: (Request, Response) -> Any = { req, res ->
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
    res.status(201)
}