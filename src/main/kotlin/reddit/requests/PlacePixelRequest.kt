package reddit.requests


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlacePixelRequest(
    @SerialName("operationName")
    val operationName: String = "setPixel",
    @SerialName("variables")
    val variables: Variables = Variables(),
    @SerialName("query")
    val query: String = "mutation setPixel(\$input: ActInput!) {  act(input: \$input) {    data {      ... on BasicMessage {        id        data {          ... on GetUserCooldownResponseMessageData {            nextAvailablePixelTimestamp            __typename          }          ... on SetPixelResponseMessageData {            timestamp            __typename          }          __typename        }        __typename      }      __typename    }    __typename  }}"
) {
    constructor(x: Int, y: Int, colorIndex: Int, canvasIndex: Int) : this(variables = Variables(input = Variables.Input(pixelMessageData = Variables.Input.PixelMessageData(coordinate = Variables.Input.PixelMessageData.Coordinate(x = x, y = y), colorIndex = colorIndex, canvasIndex = canvasIndex))))

    @Serializable
    data class Variables(
        @SerialName("input")
        val input: Input = Input()
    ) {
        @Serializable
        data class Input(
            @SerialName("actionName")
            val actionName: String = "r/replace:set_pixel",
            @SerialName("PixelMessageData")
            val pixelMessageData: PixelMessageData = PixelMessageData()
        ) {
            @Serializable
            data class PixelMessageData(
                @SerialName("coordinate")
                val coordinate: Coordinate = Coordinate(),
                @SerialName("colorIndex")
                val colorIndex: Int = 0,
                @SerialName("canvasIndex")
                val canvasIndex: Int = 0
            ) {
                @Serializable
                data class Coordinate(
                    @SerialName("x")
                    val x: Int = 0,
                    @SerialName("y")
                    val y: Int = 0
                )
            }
        }
    }
}