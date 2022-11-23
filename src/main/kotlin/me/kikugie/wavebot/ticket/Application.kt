package me.kikugie.wavebot.ticket

import com.kotlindiscord.kord.extensions.utils.env
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.kikugie.wavebot.utils.Config

@Serializable
data class Application(
    val keys: List<String>,
    val values: List<String>,

    val type: String,
    val tag: String,
    val username: String
) {
    companion object {
        private val json: Json = Json { ignoreUnknownKeys = true }
        private const val setupString =
            "https://sheets.googleapis.com/v4/spreadsheets/%s/values/%s?alt=json&key=%s"
        private val key = env("GOOGLE_API_KEY")
        private val sheet = Config.instance.sheet

        fun fetch(index: Int, type: String): Application {
            val table = requestTable(Config.instance.spreadsheets[type]!!).values
            val questions = table[0].drop(1)
            val tempAnsw = mutableListOf<String>()
            for (i in table.indices) {
                tempAnsw.add(table[index - 1].getOrElse(i + 1) { "" })
                if (tempAnsw[i] == "") {
                    tempAnsw[i] = "-"
                } else if (tempAnsw[i].length > 1024) {
                    tempAnsw[i] = tempAnsw[i].substring(0, 1021) + "..."
                }
            }
            val answers = tempAnsw.toList()

            return Application(
                keys = questions,
                values = answers,

                type = type,
                tag = answers[Config.instance.discordTagIndex],
                username = answers[Config.instance.mcNameIndex]
            )
        }

        private fun requestTable(type: String): ResponseTable {
            val client = HttpClient(CIO)

            return runBlocking {
                return@runBlocking json.decodeFromString<ResponseTable>(
                    client.get(
                        setupString.format(type, sheet, key).replace(" ", "%20")
                    ).bodyAsText()
                )
            }
        }

        @Serializable
        data class ResponseTable(
            val values: List<List<String>>
        )
    }
}
