package com.example.sunnyweather.logic.network

import com.example.sunnyweather.SunnyWeatherAppliction
import com.example.sunnyweather.logic.model.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaceService{
    @GET("v2/place?token=${SunnyWeatherAppliction.token}&lang=zh_CN")
    fun searchPlaces(@Query("query")query: String):Call<PlaceResponse>
}