# Release Guide

This file documents CI and local commands for producing release artifacts.

## Required CI secrets/placeholders

Set these in your CI provider:

- MAPS_API_KEY=YOUR_PROD_MAPS_KEY
- ANDROID_SIGNING_STORE_FILE_BASE64=BASE64_KEYSTORE_CONTENT
- ANDROID_SIGNING_STORE_PASSWORD=YOUR_STORE_PASSWORD
- ANDROID_SIGNING_KEY_ALIAS=YOUR_KEY_ALIAS
- ANDROID_SIGNING_KEY_PASSWORD=YOUR_KEY_PASSWORD

## CI build commands

Use these exact commands in CI from repo root:

1. Decode keystore (PowerShell example):

[System.IO.File]::WriteAllBytes("app/release.keystore", [System.Convert]::FromBase64String($env:ANDROID_SIGNING_STORE_FILE_BASE64))

2. Run tests + optimized release build:

./gradlew clean testDebugUnitTest assembleRelease -PENABLE_RELEASE_OPTIMIZATION=true -PMAPS_API_KEY=$env:MAPS_API_KEY

## Signing placeholders in app/build.gradle.kts

If you want signed release APKs directly from Gradle, add a signingConfigs.release block with values sourced from environment variables:

- storeFile = file("app/release.keystore")
- storePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD")
- keyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS")
- keyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD")

Then set buildTypes.release.signingConfig = signingConfigs.getByName("release").

## Local optimized release command

./gradlew clean testDebugUnitTest assembleRelease -PENABLE_RELEASE_OPTIMIZATION=true

## Artifact location

Primary APK output:

app/build/outputs/apk/release/app-release.apk
