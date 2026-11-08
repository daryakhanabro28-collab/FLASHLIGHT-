package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

object OriginalFileDeleter {
    private const val TAG = "OriginalFileDeleter"

    /**
     * Attempts to delete original unencrypted files after they have been securely
     * imported into the UV Privacy vault.
     *
     * For MediaStore items (Photos/Videos) on Android 11+ (API 30+), requests system
     * user consent via MediaStore.createDeleteRequest.
     * For SAF document URIs, deletes via DocumentsContract.deleteDocument.
     * For pre-API 30 or standard content URIs, uses ContentResolver.delete.
     */
    fun deleteOriginals(
        context: Context,
        uris: List<Uri>,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>?,
        onResult: (directCount: Int, requestedViaSystemCount: Int) -> Unit
    ) {
        var directDeleted = 0
        val mediaStoreUris = mutableListOf<Uri>()

        for (uri in uris) {
            val authority = uri.authority.orEmpty()
            val uriStr = uri.toString()
            val isMediaUri = authority.contains("media", ignoreCase = true) ||
                    uriStr.startsWith("content://media/")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isMediaUri) {
                mediaStoreUris.add(uri)
            } else {
                var deleted = false
                try {
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "DocumentsContract delete failed for $uri: ${e.message}")
                }

                if (!deleted) {
                    try {
                        val count = context.contentResolver.delete(uri, null, null)
                        if (count > 0) deleted = true
                    } catch (e: Exception) {
                        Log.w(TAG, "ContentResolver delete failed for $uri: ${e.message}")
                    }
                }

                if (deleted) directDeleted++
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mediaStoreUris.isNotEmpty()) {
            try {
                val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(context.contentResolver, mediaStoreUris)
                intentSenderLauncher?.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                onResult(directDeleted, mediaStoreUris.size)
                return
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore.createDeleteRequest failed: ${e.message}", e)
                // Fallback direct delete attempts
                for (mUri in mediaStoreUris) {
                    try {
                        val rows = context.contentResolver.delete(mUri, null, null)
                        if (rows > 0) directDeleted++
                    } catch (ex: Exception) {
                        Log.w(TAG, "Fallback delete failed for $mUri: ${ex.message}")
                    }
                }
            }
        }

        onResult(directDeleted, 0)
    }
}
