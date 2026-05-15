package com.example.database_project;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.PATCH;
import retrofit2.http.DELETE;

public interface ApiService {

    @GET("/api/user/me")
    Call<UserMeResponse> getMyInfo();

    @GET("/api/diary")
    Call<DiaryResponse> getDiaryByDate(
            @Query("target_date") String targetDate
    );

    @POST("/api/diary")
    Call<DiaryResponse> createDiary(
            @Body DiaryRequest request
    );

    @PATCH("/api/diary/{id}")
    Call<DiaryResponse> updateDiary(
            @Path("id") String id,
            @Body DiaryRequest request
    );

    @GET("/api/todo")
    Call<TodoResponse> getTodosByDate(
            @Query("target_date") String targetDate
    );

    @PATCH("/api/todo/{id}")
    Call<BasicResponse> updateTodo(
            @Path("id") int id,
            @Body TodoUpdateRequest request
    );

    @GET("/api/routine/{routineId}/items")
    Call<RoutineItemResponse> getRoutineItems(
            @Path("routineId") String routineId
    );

    @GET("/api/routine/")
    Call<RoutineResponse> getRoutines();

    @GET("/api/routine/")
    Call<RoutineResponse> getRoutines(
            @Query("target_date") String targetDate
    );

    @POST("/api/routine/{routineId}/toggle")
    Call<BasicResponse> toggleRoutine(
            @Path("routineId") String routineId,
            @Body Map<String, String> body
    );

    @GET("/api/diary/search")
    Call<DiaryListResponse> searchDiaries(@Query("keyword") String keyword);
    @GET("/api/diary/{id}")
    Call<DiaryResponse> getDiaryById(@Path("id") int id);

    // 삭제 API도 마찬가지로 수정하는 것이 안전합니다.
    @DELETE("/api/diary/{id}")
    Call<DiaryDeleteResponse> deleteDiary(@Path("id") int id);
}