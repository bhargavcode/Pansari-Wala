package org.bhargav.pansariwala.feature.printer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.printing.OrderDocument
import org.bhargav.pansariwala.printing.PosPrintService
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.print_action_invoice
import pansariwala.shared.generated.resources.print_action_menu
import pansariwala.shared.generated.resources.print_action_receipt
import pansariwala.shared.generated.resources.print_action_ticket

/** One printable thing offered by a screen. The job runs in [PosPrintService]'s app-wide queue. */
class PrintAction(
    val label: StringResource,
    val job: suspend PosPrintService.() -> Result<Unit>,
)

/** Receipt / invoice / kitchen ticket for one order. */
fun orderPrintActions(order: Order): List<PrintAction> = listOf(
    PrintAction(Res.string.print_action_receipt) { printOrder(order, OrderDocument.RECEIPT) },
    PrintAction(Res.string.print_action_invoice) { printOrder(order, OrderDocument.INVOICE) },
    PrintAction(Res.string.print_action_ticket) { printOrder(order, OrderDocument.TICKET) },
)

/** Toolbar print icon; a single action prints directly, several open a menu. */
@Composable
fun PrintMenuButton(
    actions: List<PrintAction>,
    modifier: Modifier = Modifier,
    printService: PosPrintService = koinInject(),
) {
    if (actions.isEmpty()) return
    val active by printService.activeJobs.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = {
            if (actions.size == 1) printService.enqueue(actions.first().job) else expanded = true
        }) {
            if (active > 0) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Print, contentDescription = stringResource(Res.string.print_action_menu))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.label)) },
                    leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                    onClick = {
                        expanded = false
                        printService.enqueue(action.job)
                    },
                )
            }
        }
    }
}

/** Inline row of print buttons, for detail panes. */
@Composable
fun PrintActionsRow(
    actions: List<PrintAction>,
    modifier: Modifier = Modifier,
    printService: PosPrintService = koinInject(),
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { action ->
            OutlinedButton(
                onClick = { printService.enqueue(action.job) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(action.label), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}
