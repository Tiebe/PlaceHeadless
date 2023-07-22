package reddit

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

class RedditConnection(private val client: HttpClient, private val username: String, private val password: String) {
    suspend fun login() {
        println("Logging in as $username")

        val loginPage = Jsoup.parse(client.get {
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

        val response = client.submitForm(formParameters = parameters) {
            url("https://www.reddit.com/login")
            userAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/111.0")
        }

    }
}