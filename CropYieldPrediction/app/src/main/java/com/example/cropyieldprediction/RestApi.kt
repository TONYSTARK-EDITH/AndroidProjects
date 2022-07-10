package com.example.cropyieldprediction

data class RestApi(
    val user: String,
    val n: Double,
    val p: Double,
    val k: Double,
    val rain: Double,
    val area: Double,
    val pred: String,
    val month: Double,
    val moist: Double,
    val temp: Double,
    val crop: String,
    val area_unit: String,
    val status: Int
)
