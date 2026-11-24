package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslateApiService {
    @GET("get")
    suspend fun translate(
        @Query("q") query: String,
        @Query("langpair") langPair: String = "en|vi"
    ): TranslateResponse

    companion object {
        private const val BASE_URL = "https://api.mymemory.translated.net/"

        fun create(): TranslateApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(TranslateApiService::class.java)
        }
    }
}
