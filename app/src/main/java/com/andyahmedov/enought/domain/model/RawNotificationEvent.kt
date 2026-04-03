package com.andyahmedov.enought.domain.model

import java.time.Instant

data class RawNotificationEvent(
    val id: String,
    val sourcePackage: String,
    val postedAt: Instant,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val extrasJson: String?,
    val payloadHash: String,
) {
    init {
        require(id.isNotBlank()) {
            "id must not be blank"
        }
        require(sourcePackage.isNotBlank()) {
            "sourcePackage must not be blank"
        }
        require(payloadHash.isNotBlank()) {
            "payloadHash must not be blank"
        }
    }
}

