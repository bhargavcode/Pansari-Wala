package org.bhargav.pansariwala.master

import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReportProblem
import pansariwala.shared.generated.resources.master_nav_subscriptions
import pansariwala.shared.generated.resources.master_nav_app_usage
import pansariwala.shared.generated.resources.master_nav_issues
import pansariwala.shared.generated.resources.master_nav_messages
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.navigateToLanding
import org.bhargav.pansariwala.platform.SyncBrowserHistory
import org.bhargav.pansariwala.theme.PansariTheme
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.webSessionGet
import org.bhargav.pansariwala.webSessionRemove
import org.bhargav.pansariwala.webSessionSet
import org.bhargav.pansariwala.webStorageGet
import org.bhargav.pansariwala.webStorageRemove
import org.bhargav.pansariwala.webStorageSet
import pansariwala.shared.generated.resources.master_login_required
import pansariwala.shared.generated.resources.master_network_error
import pansariwala.shared.generated.resources.master_session_expired
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.login_password
import pansariwala.shared.generated.resources.login_sign_in
import pansariwala.shared.generated.resources.login_email_username
import pansariwala.shared.generated.resources.master_admin_title
import pansariwala.shared.generated.resources.master_back_to_site
import pansariwala.shared.generated.resources.master_console_title
import pansariwala.shared.generated.resources.master_forgot_password
import pansariwala.shared.generated.resources.master_login_hint
import pansariwala.shared.generated.resources.master_menu
import pansariwala.shared.generated.resources.master_nav_dashboard
import pansariwala.shared.generated.resources.master_nav_logout
import pansariwala.shared.generated.resources.master_nav_partners
import pansariwala.shared.generated.resources.master_nav_platform
import pansariwala.shared.generated.resources.master_nav_products
import pansariwala.shared.generated.resources.master_nav_settings
import pansariwala.shared.generated.resources.master_nav_shops
import pansariwala.shared.generated.resources.master_nav_transactions
import pansariwala.shared.generated.resources.master_nav_users
import pansariwala.shared.generated.resources.master_remember_me
import pansariwala.shared.generated.resources.master_request_access

@Composable
fun MasterAdminApp() {
    val tokenKey = AppConstants.MasterWeb.TOKEN_STORAGE_KEY
    val storedToken = remember { webStorageGet(tokenKey) ?: webSessionGet(tokenKey) }
    val storedExpired = remember {
        storedToken != null && MasterSession.isExpired(storedToken, AppClock.nowMillis())
    }
    var token by remember { mutableStateOf(storedToken.takeUnless { storedExpired }) }
    var sessionExpired by remember { mutableStateOf(storedExpired) }
    fun clearSession() {
        webStorageRemove(tokenKey)
        webSessionRemove(tokenKey)
        token = null
    }
    LaunchedEffect(Unit) {
        if (storedExpired) clearSession()
    }
    LaunchedEffect(token) {
        val current = token ?: return@LaunchedEffect
        while (!MasterSession.isExpired(current, AppClock.nowMillis())) {
            delay(AppConstants.MasterWeb.SESSION_CHECK_INTERVAL_MS)
        }
        MasterSession.onExpired?.invoke()
    }
    DisposableEffect(Unit) {
        MasterSession.onExpired = {
            if (token != null) sessionExpired = true
            clearSession()
        }
        onDispose { MasterSession.onExpired = null }
    }
    PansariTheme {
        if (token.isNullOrBlank()) {
            MasterLoginScreen(
                sessionExpired = sessionExpired,
                onLoggedIn = { access, rememberMe, username ->
                    if (rememberMe) {
                        webStorageSet(tokenKey, access)
                        webSessionRemove(tokenKey)
                        webStorageSet(AppConstants.MasterWeb.REMEMBER_USER_KEY, username)
                    } else {
                        webStorageRemove(tokenKey)
                        webSessionSet(tokenKey, access)
                        webStorageRemove(AppConstants.MasterWeb.REMEMBER_USER_KEY)
                    }
                    sessionExpired = false
                    token = access
                },
            )
        } else {
            key(token) {
                MasterShell(token = token!!, onLogout = ::clearSession)
            }
        }
    }
}

@Composable
private fun MasterLoginScreen(sessionExpired: Boolean, onLoggedIn: (String, Boolean, String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var username by rememberSaveable {
        mutableStateOf(webStorageGet(AppConstants.MasterWeb.REMEMBER_USER_KEY).orEmpty())
    }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(username.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoginBackground(Modifier.fillMaxSize())
        Card(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.master_admin_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    username,
                    { username = it },
                    label = { Text(stringResource(Res.string.login_email_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = error != null && username.isBlank(),
                )
                OutlinedTextField(
                    password,
                    { password = it },
                    label = { Text(stringResource(Res.string.login_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(rememberMe, { rememberMe = it })
                    Text(stringResource(Res.string.master_remember_me))
                }
                if (sessionExpired && error == null) {
                    Text(
                        stringResource(Res.string.master_session_expired),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (hint != null) {
                    Text(hint!!, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                val requiredMsg = stringResource(Res.string.master_login_required)
                val networkMsg = stringResource(Res.string.master_network_error)
                Button(
                    enabled = !busy,
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            error = requiredMsg
                            return@Button
                        }
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val res = api.login(username.trim(), password)
                                onLoggedIn(res.accessToken, rememberMe, username.trim())
                            } catch (e: MasterApiException) {
                                error = e.message
                            } catch (e: Exception) {
                                error = networkMsg
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.login_sign_in))
                    }
                }
                val loginHint = stringResource(Res.string.master_login_hint)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { hint = loginHint }) {
                        Text(stringResource(Res.string.master_forgot_password))
                    }
                    TextButton(onClick = { hint = loginHint }) {
                        Text(stringResource(Res.string.master_request_access))
                    }
                }
                OutlinedButton(onClick = ::navigateToLanding, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.master_back_to_site))
                }
            }
        }
    }
}

@Composable
private fun MasterShell(token: String, onLogout: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStack = remember { mutableStateListOf<MasterDest>(MasterDest.Dashboard) }
    val dest = backStack.last()
    var drawerOpen by remember { mutableStateOf(false) }

    /** Going to a destination already in the stack pops back to it, so list → detail → list never loops. */
    fun go(d: MasterDest) {
        val existing = backStack.indexOf(d)
        if (existing >= 0) {
            while (backStack.size > existing + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            backStack.add(d)
        }
        drawerOpen = false
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        drawerOpen = false
    }

    SyncBrowserHistory(depth = backStack.size, onBrowserBack = ::back)

    fun report(msg: String) {
        if (msg.isBlank()) return
        scope.launch { snackbar.showSnackbar(msg) }
    }

    AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        val compact = widthClass == WindowWidthClass.Compact || widthClass == WindowWidthClass.Medium
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (!compact) {
                    MasterSidebar(
                        current = dest,
                        onNavigate = ::go,
                        onLogout = onLogout,
                        modifier = Modifier.width(AppConstants.MasterWeb.SIDEBAR_WIDTH_DP.dp).fillMaxHeight(),
                    )
                }
                Column(Modifier.fillMaxSize()) {
                    if (compact) {
                        Row(
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { drawerOpen = !drawerOpen }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.master_menu))
                            }
                            Text(stringResource(Res.string.master_console_title), style = MaterialTheme.typography.titleMedium)
                        }
                        if (drawerOpen) {
                            MasterSidebar(
                                current = dest,
                                onNavigate = ::go,
                                onLogout = onLogout,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                        CompositionLocalProvider(LocalMasterBack provides ::back) {
                            MasterRouter(
                                token = token,
                                dest = dest,
                                onNavigate = ::go,
                                onStatus = ::report,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterSidebar(
    current: MasterDest,
    onNavigate: (MasterDest) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple(MasterDest.Dashboard, Icons.Default.Dashboard, Res.string.master_nav_dashboard),
        Triple(MasterDest.Shops, Icons.Default.Store, Res.string.master_nav_shops),
        Triple(MasterDest.Products, Icons.Default.Inventory2, Res.string.master_nav_products),
        Triple(MasterDest.Users, Icons.Default.People, Res.string.master_nav_users),
        Triple(MasterDest.Partners, Icons.Default.DeliveryDining, Res.string.master_nav_partners),
        Triple(MasterDest.Transactions, Icons.AutoMirrored.Filled.ReceiptLong, Res.string.master_nav_transactions),
        Triple(MasterDest.Platform, Icons.Default.BarChart, Res.string.master_nav_platform),
        Triple(MasterDest.Subscriptions, Icons.Default.WorkspacePremium, Res.string.master_nav_subscriptions),
        Triple(MasterDest.AppUsage, Icons.Default.PhoneAndroid, Res.string.master_nav_app_usage),
        Triple(MasterDest.Issues, Icons.Default.ReportProblem, Res.string.master_nav_issues),
        Triple(MasterDest.ServerMessages, Icons.Default.Translate, Res.string.master_nav_messages),
        Triple(MasterDest.Settings, Icons.Default.Settings, Res.string.master_nav_settings),
    )
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "P",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(12.dp),
            )
            items.forEach { (target, icon, label) ->
                val selected = when (current) {
                    is MasterDest.ShopDetail, is MasterDest.ShopEdit, is MasterDest.ShopProducts, MasterDest.ShopCreate ->
                        target == MasterDest.Shops
                    is MasterDest.ProductEdit -> target == MasterDest.Products
                    is MasterDest.ProductTranslations -> target == if (current.shopId == null) MasterDest.Products else MasterDest.Shops
                    is MasterDest.TxnDetail -> target == MasterDest.Transactions
                    is MasterDest.UserDetail -> target == MasterDest.Users
                    is MasterDest.PartnerDetail -> target == MasterDest.Partners
                    else -> current == target
                }
                NavItem(stringResource(label), icon, selected) { onNavigate(target) }
            }
            Spacer(Modifier.height(16.dp))
            NavItem(stringResource(Res.string.master_nav_logout), Icons.AutoMirrored.Filled.Logout, false, onLogout)
            TextButton(onClick = ::navigateToLanding) { Text(stringResource(Res.string.master_back_to_site)) }
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun MasterRouter(
    token: String,
    dest: MasterDest,
    onNavigate: (MasterDest) -> Unit,
    onStatus: (String) -> Unit,
) {
    when (dest) {
        MasterDest.Dashboard -> DashboardScreen(token, onNavigate, onStatus)
        MasterDest.Shops -> ShopsListScreen(token, onNavigate, onStatus)
        MasterDest.ShopCreate -> ShopCreateScreen(token, onNavigate, onStatus)
        is MasterDest.ShopDetail -> ShopDetailScreen(token, dest.id, onNavigate, onStatus)
        is MasterDest.ShopEdit -> ShopEditScreen(token, dest.id, onNavigate, onStatus)
        is MasterDest.ShopProducts -> ShopProductsScreen(
            token, dest.id, dest.name, onTranslate = { onNavigate(MasterDest.ProductTranslations(dest.id, dest.name)) }, onStatus,
        )
        is MasterDest.ProductTranslations -> ProductTranslationsScreen(token, dest.shopId, dest.shopName, onStatus)
        MasterDest.Products -> ProductsListScreen(token, onNavigate, onStatus)
        is MasterDest.ProductEdit -> ProductEditScreen(token, dest.id, onNavigate, onStatus)
        MasterDest.Transactions -> TransactionsScreen(token, onNavigate, onStatus)
        is MasterDest.TxnDetail -> TxnDetailScreen(token, dest.id, onNavigate, onStatus)
        MasterDest.Users -> UsersScreen(token, onNavigate, onStatus)
        is MasterDest.UserDetail -> UserDetailScreen(token, dest.id, onNavigate, onStatus)
        MasterDest.Partners -> PartnersScreen(token, onNavigate, onStatus)
        is MasterDest.PartnerDetail -> PartnerDetailScreen(token, dest.id, onNavigate, onStatus)
        MasterDest.Platform -> PlatformScreen(token, onStatus)
        MasterDest.Subscriptions -> SubscriptionsScreen(token, onStatus)
        MasterDest.AppUsage -> AppUsageScreen(token, onStatus)
        MasterDest.Issues -> IssuesScreen(token, onStatus)
        MasterDest.ServerMessages -> ServerMessagesScreen(token, onStatus)
        MasterDest.Settings -> SettingsScreen(token, onStatus)
    }
}
