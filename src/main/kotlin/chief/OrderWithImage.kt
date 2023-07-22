package chief

import chief.responses.OrderResponse
import kotlinx.serialization.Serializable

@Serializable
data class OrderWithImage(
    val order: OrderResponse,
    val image: ByteArray,
    val priorityImage: ByteArray
) {
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