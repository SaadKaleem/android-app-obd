package com.github.saadkaleem.obd.reader.net;

import com.github.saadkaleem.obd.reader.models.DefaultResponse;
import com.github.saadkaleem.obd.reader.models.LoginResponse;
import com.github.saadkaleem.obd.reader.models.UsersResponse;
//import net.simplifiedcoding.retrofitandroidtutorial.models.LoginResponse;
//import net.simplifiedcoding.retrofitandroidtutorial.models.;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface Api {


    @FormUrlEncoded
    @POST("auth/register")
    Call<DefaultResponse> createUser(
            @Field("email") String email,
            @Field("password") String password,
            @Field("name") String name,
            @Field("source") String source,
            @Field("code") String code
    );

    @FormUrlEncoded
    @POST("auth/login")
    Call<LoginResponse> userLogin(
            @Field("email") String email,
            @Field("password") String password
    );

//    @GET("allusers")
//    Call<UsersResponse> getUsers();
//
//    @FormUrlEncoded
//    @PUT("updateuser/{id}")
//    Call<LoginResponse> updateUser(
//            @Path("id") int id,
//            @Field("email") String email,
//            @Field("name") String name,
//            @Field("school") String school
//    );
//
//    @FormUrlEncoded
//    @PUT("updatepassword")
//    Call<DefaultResponse> updatePassword(
//            @Field("currentpassword") String currentpassword,
//            @Field("newpassword") String newpassword,
//            @Field("email") String email
//    );
//
//    @DELETE("deleteuser/{id}")
//    Call<DefaultResponse> deleteUser(@Path("id") int id);

}
