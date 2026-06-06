package com.roadsaathi.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.roadsaathi.data.remote.ApiClient
import com.roadsaathi.data.remote.dto.AuthResponse
import com.roadsaathi.data.remote.dto.LoginRequest
import com.roadsaathi.data.remote.dto.RegisterRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    private fun saveAuthData(response: AuthResponse) {
        apiClient.setTokenProvider { getToken().first() }
    }

    fun login(email: String, password: String): Flow<Result<AuthResponse>> = flow {
        try {
            val response = apiClient.api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    context.dataStore.edit { prefs ->
                        prefs[TOKEN_KEY] = body.token
                        prefs[REFRESH_TOKEN_KEY] = body.refreshToken
                        prefs[USER_ID_KEY] = body.user.id
                        prefs[USER_NAME_KEY] = body.user.name
                        prefs[USER_EMAIL_KEY] = body.user.email
                        prefs[USER_PHONE_KEY] = body.user.phone
                        prefs[USER_ROLE_KEY] = body.user.role
                    }
                    saveAuthData(body)
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                emit(Result.failure(Exception("Login failed: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun register(name: String, email: String, password: String, phone: String): Flow<Result<AuthResponse>> = flow {
        try {
            val response = apiClient.api.register(RegisterRequest(name, email, password, phone))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    context.dataStore.edit { prefs ->
                        prefs[TOKEN_KEY] = body.token
                        prefs[REFRESH_TOKEN_KEY] = body.refreshToken
                        prefs[USER_ID_KEY] = body.user.id
                        prefs[USER_NAME_KEY] = body.user.name
                        prefs[USER_EMAIL_KEY] = body.user.email
                        prefs[USER_PHONE_KEY] = body.user.phone
                        prefs[USER_ROLE_KEY] = body.user.role
                    }
                    saveAuthData(body)
                    emit(Result.success(body))
                } else {
                    emit(Result.failure(Exception("Empty response body")))
                }
            } else {
                emit(Result.failure(Exception("Registration failed: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[TOKEN_KEY]
        }
    }

    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[TOKEN_KEY] != null
        }
    }
}
