package reddit.responses


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CooldownResponse(
    @SerialName("data")
    val `data`: Data = Data()
) {
    @Serializable
    data class Data(
        @SerialName("act")
        val act: Act = Act()
    ) {
        @Serializable
        data class Act(
            @SerialName("data")
            val `data`: List<Data> = listOf()
        ) {
            @Serializable
            data class Data(
                @SerialName("id")
                val id: String = "",
                @SerialName("data")
                val `data`: Data = Data()
            ) {
                @Serializable
                data class Data(
                    @SerialName("nextAvailablePixelTimestamp")
                    val nextAvailablePixelTimestamp: String = ""
                )
            }
        }
    }
}