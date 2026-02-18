package com.example.gr8math.Data.Repository

import com.example.gr8math.Data.Model.ParticipantsStateData
import com.example.gr8math.Data.Model.ProfileDisplayItem
import com.example.gr8math.Data.Model.SectionIdRes
import com.example.gr8math.Data.Model.StudentInfoSide
import com.example.gr8math.Data.Model.TeacherInfo
import com.example.gr8math.Services.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class StudentParticipantsRepository {

    private val db = SupabaseService.client

    suspend fun getParticipants(courseId: Int): Result<ParticipantsStateData> {
        return withContext(Dispatchers.IO) {
            try {

                val sectionRes = db.from("course_content")
                    .select(columns = Columns.list("section_id")) { filter { eq("id", courseId) } }
                    .decodeSingleOrNull<SectionIdRes>()

                val sectionId = sectionRes?.sectionId ?: throw Exception("Invalid Course")


                val teacherDeferred = async {
                    val adviserUser = db.from("class")
                        .select(columns = Columns.raw("adviser:user!inner(id, first_name, last_name, profile_pic, roles, birthdate)")) {
                            filter { eq("id", sectionId) }
                        }.decodeSingleOrNull<ClassAdviserRes>()?.adviser

                    if (adviserUser != null) {
                        // Get Teacher ID
                        val teacherRow = db.from("teacher").select { filter { eq("user_id", adviserUser.id) } }.decodeSingleOrNull<TeacherRow>()

                        // Get Achievements
                        val achievements = if (teacherRow != null) {
                            db.from("teacher_achievement").select { filter { eq("teacher_id", teacherRow.id) } }
                                .decodeList<AchRes>().map { ach ->
                                    ProfileDisplayItem(
                                        id = ach.id,
                                        title = ach.desc,
                                        subtitle = formatDate(ach.date),
                                        imageUrl = ach.certUrl,
                                        type = "CERTIFICATE"
                                    )
                                }
                        } else emptyList()

                        TeacherInfo(
                            userId = adviserUser.id,
                            firstName = adviserUser.firstName,
                            lastName = adviserUser.lastName,
                            profilePic = adviserUser.profilePic,
                            roles = adviserUser.roles,
                            birthdate = adviserUser.birthdate,
                            achievements = achievements
                        )
                    } else null
                }

                // 3. Fetch Students & Badges
                val studentsDeferred = async {
                    val rawStudents = db.from("student_class")
                        .select(columns = Columns.raw("""
                            student!inner (
                                id, grade_level,
                                user!inner (id, first_name, last_name, profile_pic, birthdate),
                                badges:student_badges(rank, badge_id, date_rewarded)
                            )
                        """.trimIndent())) {
                            filter { eq("class_id", sectionId) }
                        }.decodeList<StudentClassWrapper>()

                    rawStudents.map { wrapper ->
                        val topBadges = wrapper.student.badges
                            .filter { it.rank != null }
                            .sortedBy { it.rank }
                            .take(3)
                            .map { sb ->
                                ProfileDisplayItem(
                                    id = sb.badgeId,
                                    title = getBadgeName(sb.badgeId),
                                    subtitle = null,
                                    imageUrl = sb.badgeId.toString(), // Store ID to map to drawable later
                                    type = "BADGE"
                                )
                            }

                        StudentInfoSide(
                            userId = wrapper.student.user.id,
                            studentId = wrapper.student.id,
                            firstName = wrapper.student.user.firstName,
                            lastName = wrapper.student.user.lastName,
                            profilePic = wrapper.student.user.profilePic,
                            gradeLevel = wrapper.student.gradeLevel,
                            birthdate = wrapper.student.user.birthdate,
                            badges = topBadges
                        )
                    }
                }

                Result.success(ParticipantsStateData(teacherDeferred.await(), studentsDeferred.await()))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- Helpers ---
    private fun getBadgeName(id: Int): String = when(id) {
        1 -> "First Ace!"
        2 -> "First Timer"
        3 -> "First Escape"
        4 -> "Perfect Escape"
        5 -> "First Explo"
        6 -> "Full Explo"
        7 -> "3/4 Score"
        8 -> "Triple Ace"
        else -> "Badge"
    }

    private fun formatDate(dbDate: String?): String {
        if (dbDate.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dbDate)
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date!!)
        } catch (e: Exception) { dbDate }
    }

    @kotlinx.serialization.Serializable
    data class ClassAdviserRes(@kotlinx.serialization.SerialName("adviser") val adviser: UserRes)

    @kotlinx.serialization.Serializable
    data class TeacherRow(val id: Int)

    @kotlinx.serialization.Serializable
    data class AchRes(
        val id: Int,
        @kotlinx.serialization.SerialName("achievement_desc") val desc: String,
        @kotlinx.serialization.SerialName("date_acquired") val date: String?,
        @kotlinx.serialization.SerialName("certificate") val certUrl: String?
    )

    @kotlinx.serialization.Serializable
    data class StudentClassWrapper(@kotlinx.serialization.SerialName("student") val student: StudentRes)

    @kotlinx.serialization.Serializable
    data class StudentRes(
        val id: Int,
        @kotlinx.serialization.SerialName("grade_level") val gradeLevel: Int?,
        @kotlinx.serialization.SerialName("user") val user: UserRes,
        val badges: List<StudentBadgeRes> = emptyList()
    )

    @kotlinx.serialization.Serializable
    data class StudentBadgeRes(
        val rank: Int?,
        @kotlinx.serialization.SerialName("badge_id") val badgeId: Int,
        @kotlinx.serialization.SerialName("date_rewarded") val date: String?
    )

    @kotlinx.serialization.Serializable
    data class UserRes(
        val id: Int,
        @kotlinx.serialization.SerialName("first_name") val firstName: String,
        @kotlinx.serialization.SerialName("last_name") val lastName: String,
        @kotlinx.serialization.SerialName("profile_pic") val profilePic: String?,
        val roles: String? = null,
        val birthdate: String? = null
    )
}