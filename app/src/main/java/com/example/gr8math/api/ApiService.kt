package com.example.gr8math.api

import com.example.gr8math.AccountRequestResponse
import com.example.gr8math.dataObject.ClassData
import com.example.gr8math.dataObject.LoginUser
import com.example.gr8math.dataObject.User
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
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
        @Field("code") code: String,
        @Field("password") password : String,
        @Field("password_confirmation") passConfirmation: String
    ): Call<ResponseBody>


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



}