package reddit.responses


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasResponse(
    @SerialName("payload")
    val payload: Payload = Payload(),
    @SerialName("id")
    val id: String = "",
    @SerialName("type")
    val type: String = ""
) {
    @Serializable
    data class Payload(
        @SerialName("data")
        val `data`: Data = Data()
    ) {
        @Serializable
        data class Data(
            @SerialName("subscribe")
            val subscribe: Subscribe = Subscribe()
        ) {
            @Serializable
            data class Subscribe(
                @SerialName("id")
                val id: String = "",
                @SerialName("data")
                val `data`: Data = Data(),
                @SerialName("__typename")
                val typename: String = ""
            ) {
                @Serializable
                data class Data(
                    @SerialName("__typename")
                    val typename: String = "",
                    @SerialName("name")
                    val name: String = "",
                    @SerialName("timestamp")
                    val timestamp: Double = 0.0
                )
            }
        }
    }
}