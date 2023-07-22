package reddit.requests


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasRequest(
    @SerialName("id")
    val id: String = "2",
    @SerialName("type")
    val type: String = "start",
    @SerialName("payload")
    val payload: Payload = Payload()
) {
    constructor(id: String) : this(payload = CanvasRequest.Payload(variables = CanvasRequest.Payload.Variables(input = CanvasRequest.Payload.Variables.Input(channel = Payload.Variables.Input.Channel(tag = id)))))
    @Serializable
    data class Payload(
        @SerialName("variables")
        val variables: Variables = Variables(),
        @SerialName("extension")
        val extension: Extension = Extension(),
        @SerialName("operationName")
        val operationName: String = "replace",
        @SerialName("query")
        val query: String = "subscription replace(\$input: SubscribeInput!) {    subscribe(input: \$input) {        id        ... on BasicMessage {            data {                __typename                ... on FullFrameMessageData {                    __typename                    name                    timestamp                }            }            __typename        }        __typename    }}"
    ) {
        @Serializable
        data class Variables(
            @SerialName("input")
            val input: Input = Input()
        ) {
            @Serializable
            data class Input(
                @SerialName("channel")
                val channel: Channel = Channel()
            ) {
                @Serializable
                data class Channel(
                    @SerialName("teamOwner")
                    val teamOwner: String = "GARLICBREAD",
                    @SerialName("category")
                    val category: String = "CANVAS",
                    @SerialName("tag")
                    val tag: String = ""
                )
            }
        }

        @Serializable
        class Extension
    }
}