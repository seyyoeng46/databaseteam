package com.example.database_project;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RoutineApi {

    // 루틴 목록 조회
    @GET("api/routine")
    Call<RoutineResponse> getRoutines();

    //루틴 생성
    @POST("api/routine")
    Call<BasicResponse> createRoutine(@Body Map<String, Object> body);

    // 루틴 수정
    @PATCH("api/routine/{routineId}")
    Call<BasicResponse> updateRoutine(
            @Path("routineId") String routineId,
            @Body Map<String, Object> body);

    // 루틴 삭제
    @DELETE("api/routine/{routineId}")
    Call<BasicResponse> deleteRoutine(@Path("routineId") String routineId);

    // 아이템(알람) 조회
    @GET("api/routine/{routineId}/items")
    Call<ItemResponse> getItems(@Path("routineId") String routineId);

    // 아이템 추가
    @POST("api/routine/{routineId}/items")
    Call<BasicResponse> addItem(
            @Path("routineId") String routineId,
            @Body Map<String, String> body);

    // 아이템 수정
    @PATCH("api/routine/{routineId}/items/{itemId}")
    Call<BasicResponse> updateItem(
            @Path("routineId") String routineId,
            @Path("itemId") String itemId,
            @Body Map<String, String> body);

    // 아이템 삭제
    @DELETE("api/routine/{routineId}/items/{itemId}")
    Call<BasicResponse> deleteItem(
            @Path("routineId") String routineId,
            @Path("itemId") String itemId);

    // 투두 추가
    @POST("api/todo")
    Call<BasicResponse> addTodo(@Body Map<String, String> body);

    // 투두 목록 조회
    @GET("api/todo")
    Call<TodoResponse> getTodos(@Query("target_date") String targetDate);

    @GET("api/todo/all")
    Call<TodoAllResponse> getAllTodos();

    // 투두 삭제
    @DELETE("api/todo/{id}")
    Call<BasicResponse> deleteTodo(@Path("id") String id);

    // 투두 수정
    @PATCH("api/todo/{id}")
    Call<BasicResponse> patchTodo(
            @Path("id") String id,
            @Body Map<String, String> body);

    //투두 월별 출력
    @GET("api/todo/monthly")
    Call<MonthlyResponse> getMonthlyTodos(
            @Query("year") int year,
            @Query("month") int month);

    // 루틴 아이템 완료 체크/해제
    @POST("api/routine/{routineId}/items/{itemId}/complete")
    Call<BasicResponse> completeRoutineItem(
            @Path("routineId") String routineId,
            @Path("itemId") String itemId,
            @Body Map<String, Object> body);

    // 날짜별 완료 여부 조회
    @GET("api/routine/{routineId}/items/completions")
    Call<RoutineCompletionResponse> getRoutineCompletions(
            @Path("routineId") String routineId,
            @Query("completed_date") String completedDate);
}
