package chief

import chief.responses.OrderResponse
import kotlinx.serialization.Serializable
import java.awt.image.BufferedImage

data class OrderWithImage(
    val order: OrderResponse,
    val mainImage: BufferedImage,
    val priorityImage: BufferedImage? = null
) {
    fun getNextPixel(weightedDifferenceList: Map<Pair<Int, Int>, Int>): Pair<Pair<Int, Int>, Int> {
        println(weightedDifferenceList.get(weightedDifferenceList.keys.first()))
        val totalWeight = weightedDifferenceList.values.sum()

        val random = (0..totalWeight).random()
        var currentWeight = 0

        for ((pixel, weight) in weightedDifferenceList) {
            currentWeight += weight
            if (currentWeight >= random) {
                return pixel to mainImage.getRGB(pixel.first - order.offset.x, pixel.second - order.offset.y)
            }
        }
        return 0 to 0 to 0
    }

    fun getWeightedDifferenceList(image: BufferedImage): Map<Pair<Int, Int>, Int> {
        val width = order.size.width
        val height = order.size.height

        val incorrectPixels = mutableMapOf<Pair<Int, Int>, Int>()

        for (i in 0 until width) {
            for (j in 0 until height) {
                val orderPixel = mainImage.getRGB(i, j)
                val imagePixel = image.getRGB(i, j)

                val priority = if (priorityImage != null) {
                    priorityImage.getRGB(i, j) and 0x00FFFFFF
                } else {
                    0
                }

                if (orderPixel != 0 && orderPixel != imagePixel) {
                    incorrectPixels[(i + order.offset.x) to (j + order.offset.y)] = priority
                }
            }
        }

        println("Incorrect pixels: ${incorrectPixels.size}")
        return incorrectPixels
    }
}