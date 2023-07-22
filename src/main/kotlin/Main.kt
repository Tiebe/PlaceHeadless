import chief.ChiefConnection
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import reddit.RedditConnection
import java.util.*

val json = Json { encodeDefaults = true }

fun main(args: Array<String>) {
    println("Hello World!")

    val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }

        install(ContentNegotiation)
        install(HttpCookies)
    }

    val redditConnection = RedditConnection(client, "TwistSimple6647", "Tiebe1234!")
    runBlocking { redditConnection.login()
    redditConnection.getAccessToken()
        redditConnection.getCanvases(mutableListOf(0,1,2,3,4,5))
    }

/*    val chiefConnection = ChiefConnection(client)
    chiefConnection.blockingConnect()*/
}