package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.backup.PendingRestore

/**
 * Confirm dialog for restoring a parsed backup (H4 extraction from MainActivity).
 * Look and wording match the previous inline dialog.
 */
@Composable
fun RestoreConfirmDialog(
    pending: PendingRestore,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup?") },
        text = {
            Text(
                "Items in this backup replace the matching items on this phone. " +
                    "Nothing else is changed or deleted.\n${pending.preview.summaryLines()}"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("backup_restore_confirm")
            ) { Text("Restore") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("backup_restore_cancel")
            ) { Text("Cancel") }
        }
    )
}
