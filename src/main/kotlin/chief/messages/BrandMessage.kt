package chief.messages


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrandMessage(
    @SerialName("type")
    val type: String = "brand",
    @SerialName("payload")
    val payload: Payload = Payload()
) {
    @Serializable
    data class Payload(
        @SerialName("author")
        val author: String = "Tiebe",
        @SerialName("name")
        val name: String = "PlaceHeadless",
        @SerialName("version")
        val version: String = "0.0.1"
    )
}