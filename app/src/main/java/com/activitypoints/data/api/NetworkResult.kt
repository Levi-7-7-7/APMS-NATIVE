package com.activitypoints.data.api

/**
 * Sealed wrapper for all network/repository results.
 * ViewModels map this to UiState without knowing about Retrofit internals.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}

/** Convenience: safely execute a suspend Retrofit call and wrap the result. */
suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): NetworkResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                // 204 No Content is fine for Unit responses
                @Suppress("UNCHECKED_CAST")
                NetworkResult.Success(Unit as T)
            }
        } else {
            NetworkResult.Error(
                message = response.errorBody()?.string() ?: "Server error",
                code    = response.code(),
            )
        }
    } catch (e: Exception) {
        NetworkResult.Error(message = e.localizedMessage ?: "Network error")
    }
}
