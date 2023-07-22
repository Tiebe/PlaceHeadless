package reddit


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedditLoginData(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("expires")
    val expires: String,
    @SerialName("expiresIn")
    val expiresIn: Int,
    @SerialName("unsafeLoggedOut")
    val unsafeLoggedOut: Boolean,
    @SerialName("safe")
    val safe: Boolean
)