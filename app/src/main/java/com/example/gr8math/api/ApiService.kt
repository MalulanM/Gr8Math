package com.example.gr8math.api

import com.example.gr8math.AccountRequestResponse
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
}