package me.kikugie.wavebot.ticket

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.flow.filter
import me.kikugie.wavebot.guild
import me.kikugie.wavebot.utils.Config

class Ticket(app: Application) {
    val application = app
    var channel: TextChannel? = null
    var applicant: Member? = null

    suspend fun createChannel(name: String? = null): Boolean {
        val decidedName = name ?: application.username
        try {
            guild!!.createTextChannel("${application.type.substring(0, 1)}-$decidedName-application") {
                position = Int.MAX_VALUE
                parentId = Config.instance.appCategory
            }
                .also { channel = it }
        } catch (e: RestRequestException) {
            return false
        }
        return true
    }

    suspend fun addKnownApplicant(member: Member?): Member? {
        if (member != null) {
            channel!!.editMemberPermission(member.id) {
                allowed = Permissions(Permission.ViewChannel, Permission.SendMessages)
            }
            member.addRole(Config.instance.applicantRoleId)
            return member
        }
        return null
    }

    suspend fun addApplicant(tag: String = application.tag): Member? {
        val applicantData = tag.split("#")
        if (applicantData.size != 2) {
            return null
        }
        guild!!.members.filter { it.username == applicantData[0] && it.discriminator == applicantData[1] }
            .collect { applicant = it }
        return addKnownApplicant(applicant)
    }
}
