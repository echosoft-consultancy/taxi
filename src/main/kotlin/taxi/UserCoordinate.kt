package taxi

import java.time.LocalDateTime

data class UserCoordinate(val email: String, val coordinates: Coordinate, val timestamp: String = LocalDateTime.now().toString())