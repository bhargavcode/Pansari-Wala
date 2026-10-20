package org.bhargav.pansariwala.feature.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.IssueReportCreate
import org.bhargav.pansariwala.api.IssueReportDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.CameraPermissionDeniedException
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.appBuildInfo
import org.bhargav.pansariwala.platform.pickImageCatching
import org.bhargav.pansariwala.product.currentAppProduct
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_camera_permission_denied
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_photo_pick_failed
import pansariwala.shared.generated.resources.issue_submitted

enum class IssueSheetMode { REPORT, LIST }

enum class IssueTab(val status: String) {
    OPEN(AppConstants.Support.STATUS_OPEN),
    CLOSED(AppConstants.Support.STATUS_RESOLVED),
}

class IssueAttachment(val url: String, val previewBytes: ByteArray?)

data class IssueReportUiState(
    val mode: IssueSheetMode = IssueSheetMode.REPORT,
    val features: List<IssueFeature> = emptyList(),
    val feature: IssueFeature? = null,
    val issueType: IssueType? = null,
    val description: String = "",
    val attachments: List<IssueAttachment> = emptyList(),
    val uploading: Boolean = false,
    val submitting: Boolean = false,
    val tab: IssueTab = IssueTab.OPEN,
    val issues: List<IssueReportDto> = emptyList(),
    val loadingIssues: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
) {
    val canSubmit: Boolean get() = feature != null && issueType != null && !uploading && !submitting
    val canAttach: Boolean get() = attachments.size < AppConstants.Support.MAX_ATTACHMENTS && !uploading
}

class IssueReportViewModel(
    private val api: PansariApi,
    private val imagePicker: ImagePicker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IssueReportUiState(features = IssueFeature.forApp(currentAppProduct())))
    val uiState: StateFlow<IssueReportUiState> = _uiState.asStateFlow()

    fun reset() {
        _uiState.value = IssueReportUiState(features = IssueFeature.forApp(currentAppProduct()))
    }

    fun selectFeature(feature: IssueFeature) {
        _uiState.update { if (it.feature == feature) it else it.copy(feature = feature, issueType = null) }
    }

    fun clearFeature() = _uiState.update { it.copy(feature = null, issueType = null) }

    fun selectType(type: IssueType) = _uiState.update { it.copy(issueType = type) }

    fun setDescription(text: String) =
        _uiState.update { it.copy(description = text.take(AppConstants.Support.MAX_DESCRIPTION)) }

    fun removeAttachment(index: Int) =
        _uiState.update { s -> s.copy(attachments = s.attachments.filterIndexed { i, _ -> i != index }) }

    fun clearFeedback() = _uiState.update { it.copy(error = null, message = null) }

    fun attach(source: ImageSource) {
        if (!_uiState.value.canAttach) return
        viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse { err ->
                val res = if (err is CameraPermissionDeniedException) Res.string.error_camera_permission_denied
                else Res.string.error_photo_pick_failed
                _uiState.update { it.copy(error = UiText.res(res)) }
                return@launch
            } ?: return@launch
            _uiState.update { it.copy(uploading = true, error = null) }
            runCatching {
                val compressed = prepareImageForUpload(picked)
                val url = api.uploadImage(
                    prefix = ImageUploadFeature.ISSUE_ATTACHMENT.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
                IssueAttachment(url, compressed.previewBytes)
            }.onSuccess { attachment ->
                _uiState.update { it.copy(uploading = false, attachments = it.attachments + attachment) }
            }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                _uiState.update { it.copy(uploading = false, error = UiText.res(Res.string.error_image_upload_failed)) }
            }
        }
    }

    fun submit() {
        val s = _uiState.value
        val feature = s.feature ?: return
        val type = s.issueType ?: return
        if (!s.canSubmit) return
        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val build = appBuildInfo()
            runCatching {
                api.reportIssue(
                    IssueReportCreate(
                        app = currentAppProduct().name,
                        feature = feature.name,
                        featureLabel = getString(feature.label),
                        issueType = type.name,
                        issueLabel = getString(type.label),
                        description = s.description.trim(),
                        attachments = s.attachments.map { it.url },
                        platform = build.platform,
                        appVersion = build.versionName,
                    ),
                )
            }.onSuccess {
                _uiState.update {
                    IssueReportUiState(
                        features = it.features,
                        mode = IssueSheetMode.LIST,
                        message = UiText.res(Res.string.issue_submitted),
                    )
                }
                loadIssues()
            }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                _uiState.update { it.copy(submitting = false, error = e.toApiUiText()) }
            }
        }
    }

    fun showMode(mode: IssueSheetMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
        if (mode == IssueSheetMode.LIST) loadIssues()
    }

    fun selectTab(tab: IssueTab) {
        if (_uiState.value.tab == tab) return
        _uiState.update { it.copy(tab = tab, issues = emptyList()) }
        loadIssues()
    }

    fun loadIssues() {
        val tab = _uiState.value.tab
        _uiState.update { it.copy(loadingIssues = true, error = null) }
        viewModelScope.launch {
            runCatching { api.myIssues(tab.status) }
                .onSuccess { list ->
                    _uiState.update { if (it.tab == tab) it.copy(issues = list, loadingIssues = false) else it }
                }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(loadingIssues = false, error = e.toApiUiText()) }
                }
        }
    }
}
