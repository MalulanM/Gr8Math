package com.example.gr8math.Services

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import com.example.gr8math.BuildConfig

object TigrisService {
    val s3Client = S3Client {
        region = "auto"
        endpointUrl = Url.parse("https://t3.storage.dev")
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = BuildConfig.accessKeyId
            secretAccessKey = BuildConfig.secretAccessKey
        }
    }

    const val BUCKET_NAME = "app-media"
}