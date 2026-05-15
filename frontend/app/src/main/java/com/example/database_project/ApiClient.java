package com.example.database_project;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:3000/";
    //    private static final String BASE_URL = "https://planner-backend-hgty.onrender.com/";
    private static Retrofit retrofit;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        SharedPreferences prefs =
                                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

                        String token = prefs.getString("jwt_token", null);

                        Log.d("API_TOKEN", "token = " + token);

                        Request.Builder builder = original.newBuilder();

                        if (token != null && !token.isEmpty()) {
                            builder.addHeader("Authorization", "Bearer " + token);
                        }

                        Request request = builder.build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}