package chief.messages


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeMessage(
    @SerialName("type")
    val type: String = "subscribe",
    @SerialName("payload")
    val channel: String
)