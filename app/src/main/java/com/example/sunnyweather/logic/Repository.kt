package com.example.sunnyweather.logic

import android.media.Session2Command
import android.util.Log
import androidx.lifecycle.liveData
import com.example.sunnyweather.logic.dao.PlaceDao
import com.example.sunnyweather.logic.model.Place
import com.example.sunnyweather.logic.model.Weather
import com.example.sunnyweather.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.coroutines.CoroutineContext

object Repository {

    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

    fun searchPlaces(query: String) = fire(Dispatchers.IO){
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok"){
            val places = placeResponse.places
            Result.success(places)
        }else{
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO){
        coroutineScope {
            val deferredRealtime =  async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            // Rate limit exceeded 问题 尝试隔 1.5s 再发送
            delay(1500)
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            Log.d("Repository","realtimeResponse 的 await 结果：${realtimeResponse.status}")
            val dailyResponse = deferredDaily.await()
            Log.d("Repository","dailyResponse 的 await 结果：${dailyResponse.status}")
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok"){
                val weather = Weather(realtimeResponse.result.realtime,
                    dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(RuntimeException(
                    "实时天气请求的响应状态为${realtimeResponse.status}" +
                    "未来每日天气请求的响应状态为${dailyResponse.status}")
                )
            }
        }
    }

    private fun <T> fire(context:CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception){
                Result.failure<T>(e)
            }
            emit(result)
        }
}