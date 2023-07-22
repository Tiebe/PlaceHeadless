package reddit

import client
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import reddit.requests.CanvasRequest
import reddit.requests.CooldownRequest
import reddit.requests.WSAuth
import reddit.responses.CanvasResponse
import reddit.responses.CooldownResponse
import java.text.SimpleDateFormat
import java.util.Date

class RedditConnection(private val username: String, private val password: String) {
    private val redditClient = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }

        install(ContentNegotiation)
        install(HttpCookies)
    }

    private var tokens: RedditLoginData? = null

    suspend fun login() {
        println("Logging in as $username")

        val loginPage = Jsoup.parse(redditClient.get {
            url("https://www.reddit.com/login")
            userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
        }.bodyAsText())

        val loginForm = loginPage.selectFirst("input[name='csrf_token']")

        if (loginForm == null) {
            println("Could not find login form")
            return
        }

        val csrfToken = loginForm.attr("value")

        val parameters = Parameters.build {
            append("csrf_token", csrfToken)
            append("otp", "")
            append("password", password)
            append("dest", "https://www.reddit.com/")
            append("username", username)
        }

        val response = redditClient.submitForm(formParameters = parameters) {
            url("https://www.reddit.com/login")
            userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
        }

        if (response.status != HttpStatusCode.OK) {
            println("Login failed")
            return
        }

        println("Login successful")

        val mainPageRequest = redditClient.get("https://www.reddit.com/") {
            userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
        }

        tokens = getTokens(mainPageRequest.bodyAsText())
    }

    private fun getTokens(html: String): RedditLoginData? {
        val soup = Jsoup.parse(html)
        val dataJson = soup.selectFirst("script[id='data']")?.dataNodes()?.get(0)?.wholeData?.drop("window.___r = ".length)?.dropLast(1)

        if (dataJson == null) {
            println("Could not find data json")
            return null
        }

        val data = Json.decodeFromString<JsonElement>(dataJson)

        val loginData = if (data.jsonObject["user"]?.jsonObject?.get("session") != null) {
            Json.decodeFromJsonElement<RedditLoginData>(data.jsonObject["user"]!!.jsonObject["session"]!!)
        } else {
            println("Could not find access token")
            null
        }

        if (loginData != null && loginData.unsafeLoggedOut) {
            println("Unsafe logged out")
            return null
        }

        return loginData
    }

    suspend fun getAccessToken() {
        if (tokens == null) {
            login()
        }

        if (tokens == null) {
            println("Could not get access token")
            return
        }

        val expiryDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(tokens!!.expires)

        if (Date().time - 15_000 > expiryDate.time) {
            println("Access token expired, refreshing")

            val response = redditClient.get("https://www.reddit.com/r/place")

            tokens = getTokens(response.bodyAsText())
        }
    }

    fun getProxiedURL(garlicURL: String): String {
        val url = Url(garlicURL.replace(Regex("/media"), ""))
        return "https://garlic-proxy.placenl.nl${url.fullPath}?bust=${Date().time}"
    }

    suspend fun getCanvases(canvases: MutableList<Int>): MutableList<String>? {
        getAccessToken()
        if (tokens == null) {
            println("Could not get canvas url")
            return null
        }

        val urls = mutableListOf<String>()

        redditClient.webSocket("wss://gql-realtime-2.reddit.com/query", {
            header("Origin", "https://hot-potato.reddit.com")
        }) {
            send(json.encodeToString(WSAuth(tokens!!.accessToken)))

            send(json.encodeToString(CanvasRequest(canvases.removeAt(0).toString())))

            while(true) {
                val serverMessage = incoming.receive() as Frame.Text?
                if (serverMessage != null) {
                    val jsonResponse = json.decodeFromString<JsonElement>(serverMessage.readText())

                    if (jsonResponse.jsonObject["payload"]?.jsonObject?.get("data")?.jsonObject?.get("subscribe")?.jsonObject?.containsKey("data") == false) {
                        continue
                    }

                    val canvasResponse = json.decodeFromJsonElement(CanvasResponse.serializer(), jsonResponse)

                    if (canvasResponse.payload.data.subscribe.data.typename != "FullFrameMessageData")
                        continue

                    val garlicURL = canvasResponse.payload.data.subscribe.data.name
                    println("Got canvas ${getProxiedURL(garlicURL)}")
                    urls.add(getProxiedURL(garlicURL))

                    if (canvases.isEmpty()) {
                        close()
                        break
                    }

                    send(json.encodeToString(CanvasRequest(canvases.removeAt(0).toString())))
                }
            }

        }

        return urls
    }


    suspend fun getCoolDown(): CooldownResponse? {
        getAccessToken()

        if (tokens == null) {
            println("Could not get cooldown")
            return null
        }

        println(json.encodeToString(CooldownRequest()))

        println(tokens!!.accessToken)
        val requestBody = "{\"query\":\"mutation GetPersonalizedTimer{\\n  act(\\n    input: {actionName: \\\"r/replace:get_user_cooldown\\\"}\\n  ) {\\n    data {\\n      ... on BasicMessage {\\n        id\\n        data {\\n          ... on GetUserCooldownResponseMessageData {\\n            nextAvailablePixelTimestamp\\n          }\\n        }\\n      }\\n    }\\n  }\\n}\\n\\n\\nsubscription SUBSCRIBE_TO_CONFIG_UPDATE {\\n  subscribe(input: {channel: {teamOwner: GARLICBREAD, category: CONFIG}}) {\\n    id\\n    ... on BasicMessage {\\n      data {\\n        ... on ConfigurationMessageData {\\n          __typename\\n          colorPalette {\\n            colors {\\n              hex\\n              index\\n            }\\n          }\\n          canvasConfigurations {\\n            dx\\n            dy\\n            index\\n          }\\n          canvasWidth\\n          canvasHeight\\n        }\\n      }\\n    }\\n  }\\n}\\n\\n\\nsubscription SUBSCRIBE_TO_CANVAS_UPDATE {\\n  subscribe(\\n    input: {channel: {teamOwner: GARLICBREAD, category: CANVAS, tag: \\\"0\\\"}}\\n  ) {\\n    id\\n    ... on BasicMessage {\\n      id\\n      data {\\n        __typename\\n        ... on DiffFrameMessageData {\\n          currentTimestamp\\n          previousTimestamp\\n          name\\n        }\\n        ... on FullFrameMessageData {\\n          __typename\\n          name\\n          timestamp\\n        }\\n      }\\n    }\\n  }\\n}\\n\\n\\n\\n\\nmutation SET_PIXEL {\\n  act(\\n    input: {actionName: \\\"r/replace:set_pixel\\\", PixelMessageData: {coordinate: { x: 53, y: 35}, colorIndex: 3, canvasIndex: 0}}\\n  ) {\\n    data {\\n      ... on BasicMessage {\\n        id\\n        data {\\n          ... on SetPixelResponseMessageData {\\n            timestamp\\n          }\\n        }\\n      }\\n    }\\n  }\\n}\\n\\n\\n\\n\\n# subscription configuration(\$input: SubscribeInput!) {\\n#     subscribe(input: \$input) {\\n#       id\\n#       ... on BasicMessage {\\n#         data {\\n#           __typename\\n#           ... on RReplaceConfigurationMessageData {\\n#             colorPalette {\\n#               colors {\\n#                 hex\\n#                 index\\n#               }\\n#             }\\n#             canvasConfigurations {\\n#               index\\n#               dx\\n#               dy\\n#             }\\n#             canvasWidth\\n#             canvasHeight\\n#           }\\n#         }\\n#       }\\n#     }\\n#   }\\n\\n# subscription replace(\$input: SubscribeInput!) {\\n#   subscribe(input: \$input) {\\n#     id\\n#     ... on BasicMessage {\\n#       data {\\n#         __typename\\n#         ... on RReplaceFullFrameMessageData {\\n#           name\\n#           timestamp\\n#         }\\n#         ... on RReplaceDiffFrameMessageData {\\n#           name\\n#           currentTimestamp\\n#           previousTimestamp\\n#         }\\n#       }\\n#     }\\n#   }\\n# }\\n\",\"variables\":{\"input\":{\"channel\":{\"teamOwner\":\"GARLICBREAD\",\"category\":\"R_REPLACE\",\"tag\":\"canvas:0:frames\"}}},\"operationName\":\"GetPersonalizedTimer\",\"id\":null}"

        val response = client.post("https://gql-realtime-2.reddit.com/query") {
            header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
            header("Referer", "https://www.reddit.com/")
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IlNIQTI1NjpzS3dsMnlsV0VtMjVmcXhwTU40cWY4MXE2OWFFdWFyMnpLMUdhVGxjdWNZIiwidHlwIjoiSldUIn0.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxNjkwMTM0NjEyLjg4ODU3NCwiaWF0IjoxNjkwMDQ4MjEyLjg4ODU3NCwianRpIjoiTWpGSDdSS2czY2VidW9ZVDhlcDh5Vk9UV1NNMkdnIiwiY2lkIjoiOXRMb0Ywc29wNVJKZ0EiLCJsaWQiOiJ0Ml9nMDNkNWZ1YW8iLCJhaWQiOiJ0Ml9nMDNkNWZ1YW8iLCJsY2EiOjE2OTAwMzQyMzIzMDgsInNjcCI6ImVKeGtrZEdPOUNBSWhkLUZhNV9nZjVVX200MVZPa05XcFFIc1pONS1ZeXVkSm52VkEtZFQ0ZlFfWUkxVUlNQkdCQUZpU3R5YlFZQWttRE9aUWdETU5EcHJpU1FRNEVscUxHOElRQm1ia1ExWmFNY2FXM3dnQktpY0U3ZVZIcGMyb2FVYmk1NGR2NnB5TGp5cE9VZmwzTmptTFd4UDlETWJxMDJwV05aVG1jUjFwWFFXTF9vWk85UzM5dVV6X1NhMFI4T0txdkdCb3lNWThfSFpXTVppR3ZmeG5wcjBaRjB3cTczTFFXcGY2ckc3OWtXVDBESzRfUnh2dkRhVEdYSmVtcDdSX3QzMVMtakFQY19MOU5xQkdhdjdYcnJ0V2J0XzFRNVV6aWpSV0p6NE5CeTVjdmtldndUYk5lbGY0M1prTEw0WmNkTWJmbXM2T25KeDR0Q244ZlViQUFEX18xOFMyRkUiLCJyY2lkIjoiZVc4RXF5NTRaa2VRM001ZTViMEVxN2ZCUFFZck9Vc0dmd2V0cVZQU2hDSSIsImZsbyI6Mn0.qSyKszXgavxUeU35WQCHZkk9irMldKICZqIPwuwlNJZCWkd_NuZkX8pat23w05pZBUXe_WwbEh-eRaJyxCANzNKymIVIEzpxtbQpsvKAe4-uP0LiCs1Pb3DBVnyl5BIvlgm_MP0ZghlJvkRqSu-kocOPswjBNpyA71Y2Yao3ZaQWIs8lX1JVeAd4E3TwpbYNTRXTVLfPS1WQOAQg46vk9Y1QAa5qNwcwRHeBeSjJQ0gwOWxxnfOCc2TmITOp5KGkkHd2U4cP9cmnwqk5OtE9zTD2CNupKl0vn5S0o0Fho7ivFrxmDI_nCr51nYHct7JOk6ETgBkFbgfAM1uKMtCLyw")
            header("Origin", "https://www.reddit.com")
            header("Sec-Fetch-Dest", "empty")
            header("Sec-Fetch-Mode", "cors")
            header("Sec-Fetch-Site", "same-site")
            setBody(requestBody)
        }

        return response.body()
    }
}