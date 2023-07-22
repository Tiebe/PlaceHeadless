package chief.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PongMessage(
    @SerialName("type")
    val type: String = "pong"
)