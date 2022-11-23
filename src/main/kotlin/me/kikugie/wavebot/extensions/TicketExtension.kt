package me.kikugie.wavebot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import me.kikugie.wavebot.TEST_SERVER_ID
import me.kikugie.wavebot.ticket.Application
import me.kikugie.wavebot.ticket.Ticket
import me.kikugie.wavebot.utils.Config

class TicketExtension : Extension() {
    override val name = "ticket"
    val activeTickets = mutableMapOf<Snowflake, Ticket>()
    val embedGroups = listOf(
        listOf(0, 1, 2, 3, 4, 5, 6, 16), listOf(7, 8, 9, 10, 11), listOf(12, 13, 15), listOf(14)
    )

    fun MessageCreateBuilder.generateContent(application: Application) {
        embedGroups.forEach { group ->
            embed {
                color = Color(0xFF0000)
                group.forEach { index ->
                    field { name = application.keys[index]; value = application.values[index] }
                }
            }
        }
    }

    override suspend fun setup() {
        publicSlashCommand(::TicketCreateArgs) {
            name = "ticket"
            description = "Create a ticket"

            guild(TEST_SERVER_ID)

            requirePermission(Permission.ManageGuild)

            action {
                val application = Application.fetch(arguments.row, arguments.type)
                val ticket = Ticket(application)
                var appFiles = listOf<String>()
                if (application.values[17].length != 1) {
                    appFiles = application.values[17].split(", ").map {
                        "https://drive.google.com/uc?export=view&id=" + it.substring(33)
                    }
                }
                val client = HttpClient(CIO)
                println(appFiles)

                ticket.createChannel(arguments.nameOverride)
                activeTickets += ticket.channel!!.id to ticket

                ticket.channel!!.createMessage {
                    generateContent(application)
                }
                if (appFiles.isNotEmpty()) {
                    ticket.channel!!.createMessage {
                        appFiles.forEach {
                            val byteChannel = client.get(it).bodyAsChannel()
                            addFile(it.substring(44) + ".png", ChannelProvider { byteChannel })
                        }
                    }
                }
                respondEphemeral { content = "Ticket created!" }
            }
        }

        publicSlashCommand(::TicketProceedArgs) {
            name = "proceed"
            description = "Add applicant to the channel"

            guild(TEST_SERVER_ID)

            requirePermission(Permission.ManageGuild)

            action {
                val ticket = activeTickets[channel.id]
                if (ticket != null) {
                    val member = if (arguments.applicant != null) {
                        ticket.addKnownApplicant(arguments.applicant)
                    } else {
                        ticket.addApplicant(ticket.application.tag)
                    }
                    if (member != null) {
                        respondEphemeral { content = "Applicant added!" }
                        channel.createMessage {
                            content =
                                "Yo, ${member.mention}\n" + "This is your application channel\n" +
                                        "If you have any more screenshots of your builds/things that you'd like to show off you can post them here\n" +
                                        "Basically you can use this place to \"expand and enhance\" your application\n" +
                                        "You also received an ${
                                            guild!!.getRole(Config.instance.applicantRoleId).mention
                                        } role so you can join ${
                                            guild!!.getChannel(Config.instance.vcChannelId).mention
                                        }"
                        }
                    } else {
                        respondEphemeral { content = "Something went wrong!" }
                    }
                } else {
                    respondEphemeral { content = "This channel is not a ticket!" }
                }
            }
        }
    }

    inner class TicketCreateArgs : Arguments() {
        val row by int {
            name = "row"
            description = "The row to get"
        }

        val type by stringChoice {
            name = "type"
            description = "The type of application"
            choices = Config.instance.spreadsheets.keys.associateWith { it }.toMutableMap()
        }

        val nameOverride by optionalString {
            name = "name"
            description = "The name of the channel"
        }
    }

    inner class TicketProceedArgs : Arguments() {
        val applicant by optionalMember {
            name = "applicant"
            description = "Specify applicant to add"
        }
    }
}
