package me.kikugie.wavebot.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.kord.common.entity.Snowflake
import java.io.File

data class Config(
    val spreadsheets: Map<String, String>,
    val sheet: String,
    val discordTagIndex: Int,
    val mcNameIndex: Int,
    val appCategory: Snowflake,
    val archiveCategory: Snowflake,
    val applicantRoleId: Snowflake,
    val vcChannelId: Snowflake
) {
    companion object {
        private val mapper = ObjectMapper(YAMLFactory()).registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )

        val instance: Config = load()

        private fun load(): Config = mapper.readValue(File("config.yaml"), Config::class.java)
    }
}
