package chief.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderMessage(
    @SerialName("type")
    val type: String = "getOrder"
)