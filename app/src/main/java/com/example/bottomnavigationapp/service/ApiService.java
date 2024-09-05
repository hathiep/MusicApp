package com.example.bottomnavigationapp.service;

import retrofit2.Call;
import retrofit2.http.GET;

import retrofit2.http.Query;

public interface ApiService {
    @GET("search")
    Call<ApiResponse> searchSongs(
            @Query("term") String term,
            @Query("media") String media,
            @Query("entity") String entity
    );
}

