package com.example.gr8math.api

import com.example.gr8math.AccountRequestResponse
import com.example.gr8math.dataObject.AnswerPayload
import com.example.gr8math.dataObject.AssessmentRequest
import com.example.gr8math.dataObject.AssessmentResponse
import com.example.gr8math.dataObject.ClassData
import com.example.gr8math.dataObject.LoginUser
import com.example.gr8math.dataObject.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/register")
    fun registerUser(@Body user: User): Call<ResponseBody>

    @POST("api/login")
    fun loginUser(@Body userLogin: LoginUser): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/password/send-code")
    fun sendCode(@Field("email") email: String): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/password/verify-code")
    fun verifyCode(@Field("email") email: String, @Field("code") code: String): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/password/reset")
    fun savePass(
        @Field("email") email: String,
        @Field("code") code: String? = null,
        @Field("password") password : String,
        @Field("password_confirmation") passConfirmation: String
    ): Call<ResponseBody>


    @POST("api/login/update-status/{id}")
    fun updateStatus(
        @Path("id") userId: Int
    ):Call<ResponseBody>


    @POST("api/admin/store")
    fun registerAdmin(@Body user: User): Call<ResponseBody>

    @GET("api/admin/view-request")
    fun getRequest(): Call<AccountRequestResponse>

    @GET("api/admin/view-active")
    fun getActive(): Call<AccountRequestResponse>

    @POST("api/admin/accept-request/{id}")
    fun acceptRequest(@Path("id") userId: Int): Call<ResponseBody>

    @POST("api/admin/reject-request/{id}")
    fun rejectRequest(@Path("id") userId: Int): Call<ResponseBody>

    @GET("api/classes/display-class")
    fun getClasses(
        @Query("user_id") userId: Int,
        @Query("role") role: String,
        @Query("search") searchTerm : String? = null
    ): Call<ResponseBody>


    @POST("api/teacher/store")
    fun saveClass(@Body classData : ClassData): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/search/record-search-history")
    fun recordSearch(
        @Field("user_id") userId: Int,
        @Field("search_term") searchTerm: String
    ): Call<ResponseBody>

    @GET("api/search/search-history")
    fun getSearchHistory(@Query("user_id") userId: Int): Call<ResponseBody>

    @GET("api/search/suggestions")
    fun getUserSuggestions(
        @Query("user_id") userId: Int,
        @Query("q") query: String,
        @Query("role") role: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/student/join-class")
    fun joinClass(
        @Field("user_id") userId: Int,
        @Field("code") code: String
    ): Call<ResponseBody>

    @Multipart
    @POST("api/lesson/store-lesson")
    suspend fun storeLesson(
        @Part("course_id") courseId: RequestBody ,
        @Part("week_number") weekNumber: RequestBody,
        @Part("lesson_title") lessonTitle: RequestBody,
        @Part("lesson_content") lessonContent: RequestBody,
    ): Response<ResponseBody>

    @POST("api/assesment/store-assesment")
    fun storeAssessment(
        @Body assessmentRequest: AssessmentRequest
    ): Call<ResponseBody>

    @GET("api/class/display-content")
    fun getClassContent(@Query("course_id") courseId: Int): Call<ResponseBody>

    @GET("api/lesson/display-lesson")
    fun getLesson(@Query("lesson_id") lesssonId: Int): Call<ResponseBody>

    @Multipart
    @POST("api/lesson/update-lesson")
    suspend fun updateLesson(
        @Part("lesson_id") lessonId: RequestBody ,
        @Part("course_id") courseId: RequestBody ,
        @Part("week_number") weekNumber: RequestBody,
        @Part("lesson_title") lessonTitle: RequestBody,
        @Part("lesson_content") lessonContent: RequestBody,
    ): Response<ResponseBody>

    @GET("api/assesment/display-assesment")
    fun displayAssessment(
        @Query("assessment_id") assessmentId: Int
    ): Call<AssessmentResponse>

    @POST("api/student/answer-assessment")
    fun answerAssessment(@Body payload: AnswerPayload): Call<Map<String, Any>>


    @GET("api/assesment/display-score")
    fun displayScore(
        @Query("student_id") studentId :Int,
        @Query("assessment_id") assessmentId: Int
    ): Call<ResponseBody>

    @GET("api/student/with-record")
    fun hasRecord(
        @Query("student_id") userId: Int,
        @Query("assessment_id") assessmentId: Int
    ): Call<ResponseBody>
}