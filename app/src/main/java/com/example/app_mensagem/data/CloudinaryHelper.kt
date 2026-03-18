package com.example.app_mensagem.data

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryHelper {

    private const val CLOUDINARY_CLOUD_NAME = "drtexe8rh"
    const val CLOUDINARY_BASE_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME"
    private const val UPLOAD_PRESET = "chat_unsigned_preset"

    suspend fun uploadImage(uri: Uri): String {
        return uploadMedia(
            uri = uri,
            folder = "profile",
            resourceType = "image"
        )
    }

    suspend fun uploadMedia(
        uri: Uri,
        folder: String,
        publicId: String? = null,
        resourceType: String = "auto"
    ): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                var request = MediaManager.get()
                    .upload(uri)
                    .unsigned(UPLOAD_PRESET)
                    .option("folder", folder)
                    .option("resource_type", resourceType)

                if (publicId != null) {
                    request = request.option("public_id", publicId)
                }

                request.callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CloudinaryHelper", "Upload iniciado: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes > 0) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("CloudinaryHelper", "Progresso $requestId: $progress%")
                        }
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            Log.d("CloudinaryHelper", "Upload concluído: $secureUrl")
                            continuation.resume(secureUrl)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Cloudinary não retornou secure_url")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(
                            "CloudinaryHelper",
                            "Erro upload Cloudinary: ${error.description} / code=${error.code}"
                        )
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(
                            "CloudinaryHelper",
                            "Upload reagendado: ${error.description}"
                        )
                    }
                }).dispatch()

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}