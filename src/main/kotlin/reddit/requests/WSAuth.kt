package reddit.requests


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WSAuth(
    @SerialName("type")
    val type: String = "connection_init",
    @SerialName("payload")
    val payload: Payload = Payload()
) {
    constructor(token: String) : this(payload = WSAuth.Payload(authorization = "Bearer $token"))

    @Serializable
    data class Payload(
        @SerialName("Authorization")
        val authorization: String = ""
    )
}