package com.pillion.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object MapsIntentHelper {

    fun openNavigation(context: Context, lat: Double, lng: Double) {
        val navigationUri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, navigationUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

    fun openSearch(context: Context, lat: Double, lng: Double, query: String) {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val uri = Uri.parse("geo:$lat,$lng?q=$encodedQuery")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }
}
