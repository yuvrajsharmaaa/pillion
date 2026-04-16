package com.pillion.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object MapsIntentHelper {

    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    fun openNavigation(context: Context, lat: Double, lng: Double): Boolean {
        val primaryIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).apply {
            setPackage(GOOGLE_MAPS_PACKAGE)
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))
        val webFallbackUri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng".toUri()

        return launchWithFallback(
            context = context,
            primary = primaryIntent,
            fallback = fallbackIntent,
            webFallbackUri = webFallbackUri,
        )
    }

    fun openSearch(context: Context, lat: Double, lng: Double, query: String): Boolean {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val primaryIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$encodedQuery")).apply {
            setPackage(GOOGLE_MAPS_PACKAGE)
        }
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$encodedQuery"))
        val webFallbackUri = "https://www.google.com/maps/search/?api=1&query=$encodedQuery".toUri()

        return launchWithFallback(
            context = context,
            primary = primaryIntent,
            fallback = fallbackIntent,
            webFallbackUri = webFallbackUri,
        )
    }

    private fun launchWithFallback(
        context: Context,
        primary: Intent,
        fallback: Intent,
        webFallbackUri: Uri,
    ): Boolean {
        if (canHandle(context, primary)) {
            context.startActivity(primary)
            return true
        }

        if (canHandle(context, fallback)) {
            context.startActivity(fallback)
            return true
        }

        val webIntent = Intent(Intent.ACTION_VIEW, webFallbackUri)
        if (canHandle(context, webIntent)) {
            context.startActivity(webIntent)
            return true
        }

        return false
    }

    private fun canHandle(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    fun openGoogleMapsRoute(
        context: Context,
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): Boolean {
        val primaryIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$fromLat,$fromLng&destination=$toLat,$toLng"),
        ).apply {
            setPackage(GOOGLE_MAPS_PACKAGE)
        }
        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$fromLat,$fromLng&destination=$toLat,$toLng"),
        )

        return launchWithFallback(
            context = context,
            primary = primaryIntent,
            fallback = fallbackIntent,
            webFallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$fromLat,$fromLng&destination=$toLat,$toLng"),
        )
    }
}
