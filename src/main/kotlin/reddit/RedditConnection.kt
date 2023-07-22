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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import reddit.requests.CanvasRequest
import reddit.requests.CooldownRequest
import reddit.requests.PlacePixelRequest
import reddit.requests.WSAuth
import reddit.responses.CanvasResponse
import reddit.responses.CooldownResponse
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

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

    suspend fun getFullCanvas(): BufferedImage? {
        val urls = getCanvases(mutableListOf(0, 1, 2, 3, 4, 5))

        if (urls == null) {
            println("Could not get full canvas")
            return null
        }

        val canvas = BufferedImage(3000, 2000, BufferedImage.TYPE_INT_RGB)

        var offsetX = 0
        var offsetY = 0

        urls.forEachIndexed { i, url ->
            val byteSlice = client.get(url).readBytes()
            val image = ImageIO.read(ByteArrayInputStream(byteSlice))
            canvas.graphics.drawImage(image, offsetX, offsetY, null)

            offsetX += 1000

            if (i % 3 == 2) {
                offsetX = 0
                offsetY += 1000
            }
        }

        return canvas.getSubimage(500, 500, 2000, 1000)
    }


    suspend fun getCoolDown(): CooldownResponse? {
        getAccessToken()

        if (tokens == null) {
            println("Could not get cooldown")
            return null
        }

        val response = client.post("https://gql-realtime-2.reddit.com/query") {
            header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
            header("Referer", "https://www.reddit.com/")
            contentType(ContentType.Application.Json)
            bearerAuth(tokens!!.accessToken)
            header("Origin", "https://www.reddit.com")
            header("Sec-Fetch-Dest", "empty")
            header("Sec-Fetch-Mode", "cors")
            header("Sec-Fetch-Site", "same-site")
            setBody(json.encodeToString(CooldownRequest()))
        }

        return response.body()
    }

    suspend fun placePixel(x: Int, y: Int, colorIndex: Int, canvasIndex: Int) {
        getAccessToken()

        if (tokens == null) {
            println("Could not place pixel")
            return
        }

        val response = client.post("https://gql-realtime-2.reddit.com/query") {
            header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
            header("Referer", "https://www.reddit.com/")
            contentType(ContentType.Application.Json)
            bearerAuth(tokens!!.accessToken)
            header("Origin", "https://www.reddit.com")
            header("Sec-Fetch-Dest", "empty")
            header("Sec-Fetch-Mode", "cors")
            header("Sec-Fetch-Site", "same-site")
            setBody(PlacePixelRequest(x, y, colorIndex, canvasIndex))
        }

        if (response.status != HttpStatusCode.OK) {
            println("Could not place pixel")
            return
        }

        println("Placed pixel")
    }
}