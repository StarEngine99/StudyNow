package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranslateResponse(
    val responseData: ResponseData?
)

@JsonClass(generateAdapter = true)
data class ResponseData(
    val translatedText: String?
)
