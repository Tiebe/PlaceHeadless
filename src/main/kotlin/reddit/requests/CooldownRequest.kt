package reddit.requests


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CooldownRequest(
    @SerialName("query")
    val query: String = "mutation GetPersonalizedTimer{\n  act(\n    input: {actionName: \"r/replace:get_user_cooldown\"}\n  ) {\n    data {\n      ... on BasicMessage {\n        id\n        data {\n          ... on GetUserCooldownResponseMessageData {\n            nextAvailablePixelTimestamp\n          }\n        }\n      }\n    }\n  }\n}\n\n\nsubscription SUBSCRIBE_TO_CONFIG_UPDATE {\n  subscribe(input: {channel: {teamOwner: GARLICBREAD, category: CONFIG}}) {\n    id\n    ... on BasicMessage {\n      data {\n        ... on ConfigurationMessageData {\n          __typename\n          colorPalette {\n            colors {\n              hex\n              index\n            }\n          }\n          canvasConfigurations {\n            dx\n            dy\n            index\n          }\n          canvasWidth\n          canvasHeight\n        }\n      }\n    }\n  }\n}\n\n\nsubscription SUBSCRIBE_TO_CANVAS_UPDATE {\n  subscribe(\n    input: {channel: {teamOwner: GARLICBREAD, category: CANVAS, tag: \"0\"}}\n  ) {\n    id\n    ... on BasicMessage {\n      id\n      data {\n        __typename\n        ... on DiffFrameMessageData {\n          currentTimestamp\n          previousTimestamp\n          name\n        }\n        ... on FullFrameMessageData {\n          __typename\n          name\n          timestamp\n        }\n      }\n    }\n  }\n}\n\n\n\n\nmutation SET_PIXEL {\n  act(\n    input: {actionName: \"r/replace:set_pixel\", PixelMessageData: {coordinate: { x: 53, y: 35}, colorIndex: 3, canvasIndex: 0}}\n  ) {\n    data {\n      ... on BasicMessage {\n        id\n        data {\n          ... on SetPixelResponseMessageData {\n            timestamp\n          }\n        }\n      }\n    }\n  }\n}\n\n\n\n\n# subscription configuration(\$input: SubscribeInput!) {\n#     subscribe(input: \$input) {\n#       id\n#       ... on BasicMessage {\n#         data {\n#           __typename\n#           ... on RReplaceConfigurationMessageData {\n#             colorPalette {\n#               colors {\n#                 hex\n#                 index\n#               }\n#             }\n#             canvasConfigurations {\n#               index\n#               dx\n#               dy\n#             }\n#             canvasWidth\n#             canvasHeight\n#           }\n#         }\n#       }\n#     }\n#   }\n\n# subscription replace(\$input: SubscribeInput!) {\n#   subscribe(input: \$input) {\n#     id\n#     ... on BasicMessage {\n#       data {\n#         __typename\n#         ... on RReplaceFullFrameMessageData {\n#           name\n#           timestamp\n#         }\n#         ... on RReplaceDiffFrameMessageData {\n#           name\n#           currentTimestamp\n#           previousTimestamp\n#         }\n#       }\n#     }\n#   }\n# }\n",
    @SerialName("variables")
    val variables: Variables = Variables(),
    @SerialName("operationName")
    val operationName: String = "GetPersonalizedTimer",
    @SerialName("id")
    val id: String? = null
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
                val category: String = "R_REPLACE",
                @SerialName("tag")
                val tag: String = "canvas:0:frames"
            )
        }
    }
}