package chief.responses


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("message")
    val message: String = "",
    @SerialName("creator")
    val creator: Creator = Creator(),
    @SerialName("images")
    val images: Images = Images(),
    @SerialName("size")
    val size: Size = Size(),
    @SerialName("offset")
    val offset: Offset = Offset(),
    @SerialName("createdAt")
    val createdAt: String = ""
) {
    @Serializable
    data class Creator(
        @SerialName("name")
        val name: String = "",
        @SerialName("avatar")
        val avatar: String = ""
    )

    @Serializable
    data class Images(
        @SerialName("order")
        val order: String = "",
        @SerialName("priority")
        val priority: String? = null
    )

    @Serializable
    data class Size(
        @SerialName("height")
        val height: Int = 0,
        @SerialName("width")
        val width: Int = 0
    )

    @Serializable
    data class Offset(
        @SerialName("x")
        val x: Int = 0,
        @SerialName("y")
        val y: Int = 0
    )
}