package com.bpsc.flashcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bpsc.flashcards.R
import com.bpsc.flashcards.data.ReleaseInfo
import com.bpsc.flashcards.data.UpdateStatus
import com.bpsc.flashcards.ui.theme.InkType

/**
 * Non-blocking update banner for the deck list.
 *
 * Shows nothing until a newer release exists, then offers a Download button.
 * Dismissing only hides the banner for this launch — studying is never interrupted.
 */
@Composable
fun UpdateBanner(
    release: ReleaseInfo,
    status: UpdateStatus,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, scheme.primary, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_update),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "UPDATE AVAILABLE · v${release.versionName}",
                    style = InkType.eyebrow,
                    color = scheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    release.notes ?: "A newer version of this app is ready.",
                    style = InkType.body,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // One button, three jobs. A nested clickable inside a clickable parent
            // never receives the tap, so the action has to switch here instead.
            val (label, action) = when (status) {
                is UpdateStatus.Downloading -> "DOWNLOADING…" to null
                is UpdateStatus.Downloaded -> "INSTALL" to onInstall
                is UpdateStatus.Failed -> "RETRY" to onDownload
                else -> "DOWNLOAD" to onDownload
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.primary)
                    .then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                if (status is UpdateStatus.Downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = scheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = InkType.eyebrow, color = scheme.onPrimary)
                    }
                } else {
                    Text(label, style = InkType.eyebrow, color = scheme.onPrimary)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "LATER",
                style = InkType.eyebrow,
                color = scheme.outline,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}