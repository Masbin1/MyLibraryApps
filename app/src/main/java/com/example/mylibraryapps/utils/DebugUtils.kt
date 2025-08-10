package com.example.mylibraryapps.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object DebugUtils {
    private const val TAG = "DebugUtils"
    
    /**
     * Print SHA-1 fingerprint for debugging Firebase configuration
     */
    fun printSHA1Fingerprint(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            
            for (signature in packageInfo.signatures) {
                val md = MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val sha1 = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.d(TAG, "SHA-1 Fingerprint: $sha1")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SHA-1 fingerprint", e)
        }
    }
    
    /**
     * Print package information for debugging
     */
    fun printPackageInfo(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            Log.d(TAG, "Package Name: ${packageInfo.packageName}")
            Log.d(TAG, "Version Name: ${packageInfo.versionName}")
            Log.d(TAG, "Version Code: ${packageInfo.versionCode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package info", e)
        }
    }
}