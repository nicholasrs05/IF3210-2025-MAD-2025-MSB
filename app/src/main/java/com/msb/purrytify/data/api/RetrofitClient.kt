package com.msb.purrytify.data.api

import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    private val retrofit: Retrofit,
) {
    fun <T> createApiService(service: Class<T>): T {
        return retrofit.create(service)
    }
}