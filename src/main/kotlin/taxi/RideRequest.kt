package taxi

import java.time.LocalDateTime

data class RideRequest(val email: String, val timestamp: String = LocalDateTime.now().toString())

