package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuoteResponse(
    val id: Long?,
    val quote: String?,
    val author: String?
)

data class QuoteUiModel(
    val id: Long?,
    val quote: String?,
    val author: String?,
    val translation: String?
)
