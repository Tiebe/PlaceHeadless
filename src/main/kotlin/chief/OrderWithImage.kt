package chief

import chief.responses.OrderResponse
import kotlinx.serialization.Serializable

@Serializable
data class OrderWithImage(
    val order: OrderResponse,
    val image: ByteArray,
    val priorityImage: ByteArray? = null
) {
/*    fun getNextPixel() {
        //weighted shuffle
        val pixelPriorityMap = mutableMapOf<Int, Int>()

        for (i in 0 until order.size.width) {
            for (j in 0 until order.size.height) {
                val pixel = i + j * order.size.width
                val priority = order.images.priority?.get(pixel) ?: 1
                pixelPriorityMap[pixel] = priority
            }
        }
    }*/


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderWithImage

        if (order != other.order) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = order.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}