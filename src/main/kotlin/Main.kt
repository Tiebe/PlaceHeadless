import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import reddit.RedditConnection
import java.io.IOException

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
    runBlocking { redditConnection.login()
    redditConnection.getAccessToken()
        redditConnection.getCanvases(mutableListOf(0,1,2,3,4,5))
        redditConnection.getCoolDown()
    }

/*    val chiefConnection = ChiefConnection(client)
    chiefConnection.blockingConnect()*/
}