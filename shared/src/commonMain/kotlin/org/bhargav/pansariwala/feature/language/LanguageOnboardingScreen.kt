package org.bhargav.pansariwala.feature.language

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.i18n.AppLanguage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.onboarding_language_body
import pansariwala.shared.generated.resources.onboarding_language_continue
import pansariwala.shared.generated.resources.onboarding_language_default
import pansariwala.shared.generated.resources.onboarding_language_title

class LanguageOnboardingViewModel(
    private val preferences: AppPreferences,
    private val api: PansariApi,
) : ViewModel() {

    private val _selected = MutableStateFlow(AppLanguage.ENGLISH)
    val selected: StateFlow<AppLanguage> = _selected.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun select(language: AppLanguage) {
        _selected.value = language
    }

    fun confirm(onDone: () -> Unit) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val language = _selected.value
            preferences.setLanguage(language)
            preferences.setLanguageChosen(true)
            runCatching { api.saveDeviceLanguage(preferences.getOrCreateDeviceId(), language.code) }
            _saving.value = false
            onDone()
        }
    }
}

@Composable
fun LanguageOnboardingScreen(
    onContinue: () -> Unit,
    viewModel: LanguageOnboardingViewModel = koinViewModel(),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val locals = AppLanguage.entries.filter { it != AppLanguage.ENGLISH }
    PansariScreen {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.onboarding_language_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.onboarding_language_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LanguageCard(
                language = AppLanguage.ENGLISH,
                selected = selected == AppLanguage.ENGLISH,
                badge = stringResource(Res.string.onboarding_language_default),
                onClick = { viewModel.select(AppLanguage.ENGLISH) },
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(locals, key = { it.code }) { language ->
                    LanguageCard(
                        language = language,
                        selected = selected == language,
                        badge = null,
                        onClick = { viewModel.select(language) },
                    )
                }
            }
            Button(
                onClick = { viewModel.confirm(onContinue) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.onboarding_language_continue))
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    language.displayLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                )
                if (language != AppLanguage.ENGLISH) {
                    Text(
                        language.englishName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
