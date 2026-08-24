package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.navigateToLanding
import org.bhargav.pansariwala.theme.PansariTheme
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.login_password
import pansariwala.shared.generated.resources.login_sign_in
import pansariwala.shared.generated.resources.login_username
import pansariwala.shared.generated.resources.master_admin_title
import pansariwala.shared.generated.resources.master_back_to_site
import pansariwala.shared.generated.resources.master_console_title
import pansariwala.shared.generated.resources.master_module_categories
import pansariwala.shared.generated.resources.master_module_features
import pansariwala.shared.generated.resources.master_module_payments
import pansariwala.shared.generated.resources.master_module_products
import pansariwala.shared.generated.resources.master_module_reports
import pansariwala.shared.generated.resources.master_module_shop_types
import pansariwala.shared.generated.resources.master_module_shops
import pansariwala.shared.generated.resources.master_module_users
import pansariwala.shared.generated.resources.master_modules_planned

@Composable
fun MasterAdminApp() {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    PansariTheme {
        if (!loggedIn) {
            MasterLoginScreen(onLoggedIn = { loggedIn = true })
        } else {
            MasterDashboardShell()
        }
    }
}

@Composable
private fun MasterLoginScreen(onLoggedIn: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.master_admin_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(Res.string.login_username)) },
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.login_password)) },
        )
        Button(onClick = onLoggedIn, modifier = Modifier.fillMaxWidth(0.5f)) {
            Text(stringResource(Res.string.login_sign_in))
        }
        Button(onClick = ::navigateToLanding) {
            Text(stringResource(Res.string.master_back_to_site))
        }
    }
}

@Composable
private fun MasterDashboardShell() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(Res.string.master_console_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(Res.string.master_modules_planned), style = MaterialTheme.typography.titleMedium)
        listOf(
            Res.string.master_module_products,
            Res.string.master_module_categories,
            Res.string.master_module_shop_types,
            Res.string.master_module_shops,
            Res.string.master_module_reports,
            Res.string.master_module_payments,
            Res.string.master_module_users,
            Res.string.master_module_features,
        ).forEach { Text("• ${stringResource(it)}") }
    }
}
