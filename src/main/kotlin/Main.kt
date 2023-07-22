import chief.ChiefConnection
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import reddit.RedditConnection

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
    runBlocking { redditConnection.login() }

/*    val chiefConnection = ChiefConnection(client)
    chiefConnection.blockingConnect()*/
}