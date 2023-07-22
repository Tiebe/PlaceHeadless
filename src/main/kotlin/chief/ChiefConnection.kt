package chief

import chief.messages.BrandMessage
import chief.messages.OrderMessage
import chief.messages.PongMessage
import chief.messages.SubscribeMessage
import chief.responses.OrderResponse
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.websocket.*
import json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Wrapper for the connection to the Chief
 *
 * @param client The HttpClient to use for the connection. Requires websockets.
 */
class ChiefConnection(private val client: HttpClient) {
    var currentOrder: OrderWithImage? = null
    val orderListeners: MutableList<(OrderWithImage) -> Unit> = mutableListOf()

    private lateinit var webSocketSession: DefaultWebSocketSession

    suspend fun connect() {
        client.webSocket("wss://chief.placenl.nl/ws") {
            webSocketSession = this
            sendBrand()

            subscribeToChannel("announcements")
            subscribeToChannel("orders")

            getOrders()

            while(true) {
                val serverMessage = incoming.receive() as? Frame.Text
                if (serverMessage != null) {
                    val message = json.decodeFromString<JsonObject>(serverMessage.readText())
                    handleMessage(message["type"]!!.jsonPrimitive.content, message["payload"])
                }
            }
        }
    }

    fun blockingConnect() = runBlocking { connect() }

    private suspend fun handleMessage(type: String, payload: JsonElement?) {
        when(type) {
            "hello" -> {}
            "ping" -> respondPing()
            "announcement" -> println("Received announcement: $payload")

            "order" -> handleOrder(payload!!)

            "brandUpdated" -> {}
            "subscribed" -> println("Subscribed to channel: ${payload?.jsonPrimitive?.content}")
            else -> {
                println("Received unknown message type: $type, message: $payload")
            }
        }
    }

    private suspend fun sendBrand() {
        webSocketSession.send(Frame.Text(json.encodeToString(BrandMessage())))
    }

    private suspend fun respondPing() {
        webSocketSession.send(Frame.Text(json.encodeToString(PongMessage())))
    }

    private suspend fun subscribeToChannel(channel: String) {
        webSocketSession.send(Frame.Text(json.encodeToString(SubscribeMessage(channel = channel))))
    }

    suspend fun getOrders() {
        webSocketSession.send(Frame.Text(json.encodeToString(OrderMessage())))
    }

    private suspend fun handleOrder(order: JsonElement) {
        val orderResponse = json.decodeFromJsonElement(OrderResponse.serializer(), order)

        val orderImage = BufferedImage(orderResponse.size.width, orderResponse.size.height, BufferedImage.TYPE_INT_ARGB)
        orderImage.graphics.drawImage(ImageIO.read(ByteArrayInputStream(client.get(orderResponse.images.order).readBytes())), 0, 0, null)
        currentOrder = if (orderResponse.images.priority != null) {
            val priorityImage = BufferedImage(orderResponse.size.width, orderResponse.size.height, BufferedImage.TYPE_INT_ARGB)
            priorityImage.graphics.drawImage(ImageIO.read(ByteArrayInputStream(client.get(orderResponse.images.priority).readBytes())), 0, 0, null)
            OrderWithImage(orderResponse, orderImage, priorityImage)
        }
        else {
            OrderWithImage(orderResponse, orderImage)
        }

        orderListeners.forEach { it(currentOrder!!) }
    }

}