package reddit

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import reddit.requests.WSAuth
import reddit.requests.createCanvasRequest
import reddit.responses.CanvasResponse
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import kotlin.random.Random

class RedditConnection(private val client: HttpClient, private val username: String, private val password: String) {
    private var tokens: RedditLoginData? = null

    suspend fun login() {
        println("Logging in as $username")

        val loginPage = Jsoup.parse(client.get {
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

        val response = client.submitForm(formParameters = parameters) {
            url("https://www.reddit.com/login")
            userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
        }

        if (response.status != HttpStatusCode.OK) {
            println("Login failed")
            return
        }

        println("Login successful")

        val mainPageRequest = client.get("https://www.reddit.com/") {
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

            val response = client.get("https://www.reddit.com/r/place")

            tokens = getTokens(response.bodyAsText())
        }
    }

    suspend fun getCanvases(canvases: MutableList<Int>) {
        getAccessToken()
        if (tokens == null) {
            println("Could not get canvas url")
            return
        }

        client.webSocket("wss://gql-realtime-2.reddit.com/query", {
            header("Origin", "https://hot-potato.reddit.com")
        }) {
            send(json.encodeToString(WSAuth(tokens!!.accessToken)))

            send(json.encodeToString(createCanvasRequest(canvases.removeAt(0).toString())))

            while(true) {
                val serverMessage = incoming.receive() as Frame.Text?
                if (serverMessage != null) {
                    val json = json.decodeFromString<JsonElement>(serverMessage.readText())

                    if (json.jsonObject["payload"]?.jsonObject?.get("data")?.jsonObject?.get("subscribe")?.jsonObject?.containsKey("data") == false) {
                        continue
                    }

                    val canvasResponse = Json.decodeFromJsonElement(CanvasResponse.serializer(), json)

                    if (canvasResponse.payload.data.subscribe.typename != "FullFrameMessageData")
                        continue

                    println("Got canvas ${canvasResponse.payload.data.subscribe.data}")


                }
            }

        }

    }
}