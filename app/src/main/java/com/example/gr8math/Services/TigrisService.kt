package com.example.gr8math.Services

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url

object TigrisService {
    val s3Client = S3Client {
        region = "auto"
        endpointUrl = Url.parse("https://t3.storage.dev")
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "tid_bEjdWz_EtzpuuDfdkPwuD_LoAa_HWrjTlDepHqXgnTncXdqnyh"
            secretAccessKey = "tsec_nWgySDb3id1r3fNMmHEjTIv8TTuXv8s1sxjq_+sUjOX3Py16UzI6h-Z+qImjxQyGKNG_OR"
        }
    }

    const val BUCKET_NAME = "app-media"
}