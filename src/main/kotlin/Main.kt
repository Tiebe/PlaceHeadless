import chief.ChiefConnection
import chief.OrderWithImage
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import reddit.RedditConnection
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*

val json = Json { encodeDefaults = true }

val client = HttpClient {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }

    install(ContentNegotiation)
    install(HttpCookies)
}

fun main(args: Array<String>) {
    val redditConnection = RedditConnection("TwistSimple6647", "Tiebe1234!")
    val image = runBlocking { redditConnection.login()
        redditConnection.getAccessToken()
        //val urls = redditConnection.getCanvases(mutableListOf(0,1,2,3,4,5))
        redditConnection.getFullCanvas()
    }

    val chiefConnection = ChiefConnection(client)
    runBlocking {
        launch {
            chiefConnection.orderListeners.add {
                println("Received order: $it")
                if (image == null) return@add
                placeIncorrectPixel(redditConnection, chiefConnection)
            }

            chiefConnection.connect()
        }
    }
}

fun placeIncorrectPixel(redditConnection: RedditConnection, chiefConnection: ChiefConnection) {
    while (true) {
        runBlocking {
            launch {
                val currentOrder = chiefConnection.currentOrder
                println(currentOrder?.order?.offset)
                val currentMap = redditConnection.getFullCanvas()

                if (currentOrder == null || currentMap == null) return@launch

                val nextPixel = currentOrder.getNextPixel(currentOrder.getWeightedDifferenceList(currentMap))

                println(nextPixel)
                val color = "#" + Integer.toHexString(nextPixel.second).substring(2).uppercase(Locale.getDefault())
                println(color)

                val colorIndex = redditConnection.palette.indexOf(color)
                println(colorIndex)

                val canvasIndex = redditConnection.getCanvasIndex(nextPixel.first.first - currentOrder.order.offset.x, nextPixel.first.second - currentOrder.order.offset.y)
                val pixelIndex =
                    redditConnection.getPixelIndex(nextPixel.first.first - currentOrder.order.offset.x, nextPixel.first.second - currentOrder.order.offset.y, canvasIndex)

                println(canvasIndex)
                println(pixelIndex)


                //redditConnection.placePixel(nextPixel.first.first, nextPixel.first.second, nextPixel.second)
            }
            delay(10000)

        }
    }
}