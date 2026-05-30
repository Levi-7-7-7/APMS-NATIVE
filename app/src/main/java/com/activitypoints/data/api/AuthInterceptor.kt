package com.activitypoints.data.api

import com.activitypoints.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that attaches the correct JWT to every outgoing request.
 *
 * Student endpoints  → "Bearer <studentToken>"
 * Tutor endpoints    → "Bearer <tutorToken>"
 *
 * The interceptor peeks at the URL path to decide which token to use,
 * exactly like the RN app's separate axiosInstance / tutorAxios instances.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path    = request.url.encodedPath  // e.g. "/api/tutors/students"

        val token: String? = runBlocking {
            if (path.contains("/tutors/")) tokenStore.getTutorToken()
            else tokenStore.getStudentToken()
        }

        val newRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)

        // Auto-clear tokens on 401 (mirrors RN interceptor behaviour)
        if (response.code == 401) {
            runBlocking {
                if (path.contains("/tutors/")) tokenStore.clearTutorSession()
                else tokenStore.clearStudentSession()
            }
        }

        return response
    }
}
