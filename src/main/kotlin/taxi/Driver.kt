package taxi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.searchbox.core.Index
import io.searchbox.core.Search
import mu.KotlinLogging
import spark.Request
import spark.Response

private val logger = KotlinLogging.logger {}

val addDriverCoordinate: (Request, Response) -> String = { req, res ->
    // Parse coordinates from json
    val coordinates = jacksonObjectMapper().readValue<Coordinate>(req.body())
    logger.info { "Adding driver coordinates: $coordinates" }

    val driverEmail = "test@example.com"
    storeDriverCoordinates(UserCoordinate(email = driverEmail,
            coordinates = coordinates))
    res.status(201)
    noBody
}

val getRideRequests: (Request, Response) -> String = { req, res ->
    // Parse coordinates from json
    val lat = req.queryParams("lat")
    val lon = req.queryParams("lon")
    logger.info { "Getting ride requests lat: $lat, lon: $lon" }

    if (lat == null || lon == null) {
        res.status(400)
        noBody
    } else {
        val rideRequesters = rideRequests().map { it.email }
        val coordinates = latestPassengerCoordinates(lat, lon, rideRequesters)
        jacksonObjectMapper().writeValueAsString(coordinates)
    }
}

val offerRide: (Request, Response) -> String = { req, res ->
    // Parse coordinates from json
    val passengerEmail = jacksonObjectMapper().readTree(req.body()).findValue("email").asText()
    logger.info { "Offering ride to: $passengerEmail" }
    val driverEmail = "test@example.com"

    storeRide(Ride(passenger = passengerEmail,
            driver = driverEmail))
    res.status(201)
    noBody
}

private fun storeDriverCoordinates(passengerCoordinate: UserCoordinate) {
    // Add new driver coordinates to elasticsearch driver mapping
    jestClient.execute(Index.Builder(passengerCoordinate).index("taxi").type("driver").build())
}

private fun storeRide(ride: Ride) {
    // Add new driver coordinates to elasticsearch driver mapping
    jestClient.execute(Index.Builder(ride).index("taxi").type("ride").build())
}

private fun rideRequests() : List<RideRequest> {
    val query = """
{
    "query" : {
        "constant_score" : {
            "filter" : {
                "range" : {
                    "_timestamp" : {
                        "gt" : "now-1h"
                    }
                }
            }
        }
    }
}
"""
    val search = Search.Builder(query)
            .addIndex("taxi")
            .addType("ride")
            .build()
    val searchResult = jestClient.execute(search)
    return searchResult.getHits(RideRequest::class.java).map { it.source }
}

private fun latestPassengerCoordinates(lat: String, lon: String, rideRequesters: List<String>) : List<UserCoordinate> {
    // TODO Should only search over emails
    val query = """
{
   "query":{
      "bool":{
         "filter":{
            "geo_distance":{
               "distance":"10km",
               "coordinates":{
                  "lat": $lat,
                  "lon": $lon
               }
            }
         }
      }
   },
   "aggs":{
      "latest_by_email":{
         "terms":{
            "field":"email"
         },
         "aggs":{
            "latest":{
               "top_hits": {
                    "size": 1,
                    "sort": [ { "timestamp": { "order": "desc" } } ]
                 }
            }
         }
      }
   }
}
"""
    val search = Search.Builder(query)
            .addIndex("taxi")
            .addType("passenger")
            .build()
    val searchResult = jestClient.execute(search)
    return searchResult.aggregations
            .getTermsAggregation("latest_by_email")
            .buckets
            .map { it.getTopHitsAggregation("latest")
                    .getHits(UserCoordinate::class.java)[0].source
            }
}