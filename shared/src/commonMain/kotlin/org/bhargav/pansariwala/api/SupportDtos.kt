package org.bhargav.pansariwala.api

import kotlinx.serialization.Serializable

@Serializable
data class AppInstallRequest(
    val app: String,
    val platform: String,
    val versionName: String,
    val versionCode: Long = 0L,
    val deviceId: String? = null,
)

@Serializable
data class IssueReportCreate(
    val app: String,
    val feature: String,
    val featureLabel: String = "",
    val issueType: String,
    val issueLabel: String = "",
    val description: String = "",
    val attachments: List<String> = emptyList(),
    val platform: String = "",
    val appVersion: String = "",
)

@Serializable
data class IssueReportDto(
    val id: String,
    val app: String = "",
    val feature: String = "",
    val featureLabel: String = "",
    val issueType: String = "",
    val issueLabel: String = "",
    val description: String = "",
    val attachments: List<String> = emptyList(),
    val appVersion: String = "",
    val status: String = "",
    val createdAt: Long = 0L,
    val resolvedAt: Long? = null,
    val fixedInVersion: String = "",
    val resolutionNote: String = "",
)
