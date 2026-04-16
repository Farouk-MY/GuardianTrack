package com.farouk.guardiantrack.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds the API key header to every request.
 * The API key is stored securely and never committed to version control.
 */
class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Content-Type", "application/json")
            .apply {
                if (apiKey.isNotBlank()) {
                    header("x-api-key", apiKey)
                }
            }
            .method(original.method, original.body)
            .build()
        return chain.proceed(request)
    }
}
