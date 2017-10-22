package taxi

import java.time.LocalDateTime

data class Ride(val passenger: String, val driver: String, val timestamp: String = LocalDateTime.now().toString())