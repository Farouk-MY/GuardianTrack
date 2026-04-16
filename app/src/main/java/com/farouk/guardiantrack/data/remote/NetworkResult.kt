package com.farouk.guardiantrack.data.remote

/**
 * Sealed class modeling network operation states.
 * Required by specification for Retrofit responses.
 *
 * Usage:
 *   sealed class NetworkResult<T> {
 *       data class Success<T>(val data: T) : NetworkResult<T>()
 *       data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
 *       class Loading<T> : NetworkResult<T>()
 *   }
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}
