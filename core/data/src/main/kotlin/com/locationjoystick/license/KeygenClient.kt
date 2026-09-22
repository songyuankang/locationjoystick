package com.locationjoystick.license

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "KeygenClient"
private const val ACCOUNT_ID = "4bd6e61b-ac3a-4761-bc70-fe283865db60"
private const val BASE_URL = "https://api.keygen.sh/v1/accounts/$ACCOUNT_ID"
private const val MEDIA_TYPE_JSON_API = "application/vnd.api+json"

@Singleton
class KeygenClient
    @Inject
    constructor() {
        private val okHttpClient: OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

        /**
         * Validates the [licenseKey] for the given device [fingerprint].
         */
        suspend fun validateKey(
            licenseKey: String,
            fingerprint: String,
        ): LicenseValidationResult =
            withContext(Dispatchers.IO) {
                val cleanKey = licenseKey.trim()
                if (cleanKey.isEmpty()) {
                    return@withContext LicenseValidationResult.Invalid(
                        code = "EMPTY_KEY",
                        detail = "Key is empty",
                        chineseMessage = "请输入有效的授权卡密",
                    )
                }

                val requestJson =
                    JSONObject().apply {
                        put(
                            "meta",
                            JSONObject().apply {
                                put("key", cleanKey)
                                put(
                                    "scope",
                                    JSONObject().apply {
                                        put("fingerprint", fingerprint)
                                    },
                                )
                            },
                        )
                    }

                val request =
                    Request.Builder()
                        .url("$BASE_URL/licenses/actions/validate-key")
                        .addHeader("Content-Type", MEDIA_TYPE_JSON_API)
                        .addHeader("Accept", MEDIA_TYPE_JSON_API)
                        .post(requestJson.toString().toRequestBody(MEDIA_TYPE_JSON_API.toMediaType()))
                        .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""
                        if (!response.isSuccessful && responseBody.isEmpty()) {
                            return@withContext LicenseValidationResult.Invalid(
                                code = "HTTP_${response.code}",
                                detail = "HTTP request failed with status code ${response.code}",
                                chineseMessage = parseHttpErrorMessage(response.code),
                            )
                        }

                        val json =
                            try {
                                JSONObject(responseBody)
                            } catch (_: Exception) {
                                JSONObject()
                            }

                        val meta = json.optJSONObject("meta") ?: JSONObject()
                        val isValid = meta.optBoolean("valid", false)
                        val code = meta.optString("code", "")
                        val detail = meta.optString("detail", "")
                        val dataObj = json.optJSONObject("data")
                        val licenseId = dataObj?.optString("id", "") ?: ""
                        val attributes = dataObj?.optJSONObject("attributes")
                        val expiry = if (attributes != null && !attributes.isNull("expiry")) attributes.optString("expiry") else null

                        if (isValid) {
                            return@withContext LicenseValidationResult.Valid(
                                licenseId = licenseId,
                                expiry = expiry,
                                detail = detail,
                            )
                        }

                        // Check if key exists but needs machine activation
                        if (licenseId.isNotEmpty() && isNeedsActivationCode(code)) {
                            return@withContext LicenseValidationResult.NeedsMachineActivation(
                                licenseId = licenseId,
                                detail = detail,
                            )
                        }

                        // Parse errors
                        val chineseMsg = parseKeygenErrorCode(code, responseBody)
                        return@withContext LicenseValidationResult.Invalid(
                            code = code.ifEmpty { "HTTP_${response.code}" },
                            detail = detail,
                            chineseMessage = chineseMsg,
                        )
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network connection error validating license", e)
                    return@withContext LicenseValidationResult.NetworkError("无法连接授权服务器，请检查网络后重试")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error validating license", e)
                    return@withContext LicenseValidationResult.Invalid(
                        code = "UNEXPECTED_ERROR",
                        detail = e.message ?: "",
                        chineseMessage = "授权验证异常，请重试或联系客服",
                    )
                }
            }

        /**
         * Binds/activates the machine [fingerprint] for [licenseId] using the [licenseKey].
         */
        suspend fun activateMachine(
            licenseKey: String,
            licenseId: String,
            fingerprint: String,
        ): MachineActivationResult =
            withContext(Dispatchers.IO) {
                val requestJson =
                    JSONObject().apply {
                        put(
                            "data",
                            JSONObject().apply {
                                put("type", "machines")
                                put(
                                    "attributes",
                                    JSONObject().apply {
                                        put("fingerprint", fingerprint)
                                        put("name", "Android Device")
                                        put("platform", "Android")
                                    },
                                )
                                put(
                                    "relationships",
                                    JSONObject().apply {
                                        put(
                                            "license",
                                            JSONObject().apply {
                                                put(
                                                    "data",
                                                    JSONObject().apply {
                                                        put("type", "licenses")
                                                        put("id", licenseId)
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }

                val request =
                    Request.Builder()
                        .url("$BASE_URL/machines")
                        .addHeader("Content-Type", MEDIA_TYPE_JSON_API)
                        .addHeader("Accept", MEDIA_TYPE_JSON_API)
                        .addHeader("Authorization", "License ${licenseKey.trim()}")
                        .post(requestJson.toString().toRequestBody(MEDIA_TYPE_JSON_API.toMediaType()))
                        .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""

                        if (response.isSuccessful) {
                            val json = JSONObject(responseBody)
                            val machineId = json.optJSONObject("data")?.optString("id", "") ?: ""
                            return@withContext MachineActivationResult.Success(machineId)
                        }

                        val errorsArray =
                            try {
                                JSONObject(responseBody).optJSONArray("errors") ?: JSONArray()
                            } catch (_: Exception) {
                                JSONArray()
                            }

                        var errorCode = ""
                        var errorDetail = ""
                        if (errorsArray.length() > 0) {
                            val firstError = errorsArray.optJSONObject(0)
                            errorCode = firstError?.optString("code", "") ?: ""
                            errorDetail = firstError?.optString("detail", "") ?: ""
                        }

                        val chineseMsg =
                            if (errorCode == "MACHINE_LIMIT_EXCEEDED" || responseBody.contains("MACHINE_LIMIT_EXCEEDED")) {
                                "该卡密已绑定其他设备，如需换机请联系管理员解绑。"
                            } else {
                                parseKeygenErrorCode(errorCode, responseBody)
                            }

                        return@withContext MachineActivationResult.Failed(
                            code = errorCode.ifEmpty { "HTTP_${response.code}" },
                            detail = errorDetail,
                            chineseMessage = chineseMsg,
                        )
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network connection error activating machine", e)
                    return@withContext MachineActivationResult.NetworkError("无法连接授权服务器，请检查网络后重试")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error activating machine", e)
                    return@withContext MachineActivationResult.Failed(
                        code = "ACTIVATION_ERROR",
                        detail = e.message ?: "",
                        chineseMessage = "设备激活异常，请检查网络或重试",
                    )
                }
            }

        private fun isNeedsActivationCode(code: String): Boolean {
            return when (code.uppercase()) {
                "NO_MACHINES", "NO_MACHINE", "FINGERPRINT_SCOPE_MISMATCH", "NOT_ACTIVATED" -> true
                else -> false
            }
        }

        private fun parseKeygenErrorCode(
            code: String,
            responseBody: String,
        ): String {
            if (responseBody.contains("MACHINE_LIMIT_EXCEEDED")) {
                return "该卡密已绑定其他设备，如需换机请联系管理员解绑。"
            }
            return when (code.uppercase()) {
                "NOT_FOUND", "NO_MATCH" -> "卡密不存在，请检查后重试"
                "EXPIRED" -> "该卡密已过期，请购买新卡密"
                "SUSPENDED" -> "该卡密已被暂停/封禁"
                "MACHINE_LIMIT_EXCEEDED" -> "该卡密已绑定其他设备，如需换机请联系管理员解绑。"
                "NO_MACHINES", "NO_MACHINE", "FINGERPRINT_SCOPE_MISMATCH" -> "该设备尚未绑定卡密"
                else -> "授权验证未通过 ($code)"
            }
        }

        private fun parseHttpErrorMessage(statusCode: Int): String {
            return when (statusCode) {
                401, 403 -> "卡密无效或未授权"
                404 -> "卡密不存在，请检查输入"
                422 -> "设备激活失败或超出设备限制"
                429 -> "请求频率过高，请稍后再试"
                in 500..599 -> "授权服务器繁忙，请稍后再试"
                else -> "无法连接授权服务 ($statusCode)"
            }
        }
    }
