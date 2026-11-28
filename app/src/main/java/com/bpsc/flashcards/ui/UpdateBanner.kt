package com.bpsc.flashcards.ui

import androidx.compose.foundation.background
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
import com.bpsc.flashcards.ui.theme.LocalPaperColors
import com.bpsc.flashcards.ui.theme.PaperType

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
    val colors = LocalPaperColors.current
    val progress = (status as? UpdateStatus.Downloading)?.progress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.sand)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_update),
                contentDescription = null,
                tint = colors.greenText,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Update available · v${release.versionName}",
                    style = PaperType.tag,
                    color = colors.greenText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    release.notes ?: "A newer version of this app is ready.",
                    style = PaperType.small,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // One button, three jobs. A nested clickable inside a clickable parent
            // never receives the tap, so the action has to switch here instead.
            // UPDATE -> (download, button shows circular progress) -> INSTALL -> app update.
            val (label, action) = when (status) {
                is UpdateStatus.Downloading ->
                    (if (progress != null && progress > 0.01f)
                        "${(progress * 100).toInt()}%"
                    else "CONNECTING…") to null
                is UpdateStatus.Downloaded -> "INSTALL" to onInstall
                is UpdateStatus.Failed -> "RETRY" to onDownload
                else -> "UPDATE" to onDownload
            }
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.yellow)
                    .then(if (action != null) Modifier.clickable(onClick = action) else Modifier)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (status is UpdateStatus.Downloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Circular progress bar on the button; determinate once the
                        // server told us the size, indeterminate until then.
                        if (progress != null && progress > 0f) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = colors.onYellow,
                                trackColor = colors.onYellow.copy(alpha = 0.25f),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            CircularProgressIndicator(
                                color = colors.onYellow,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = PaperType.count, color = colors.onYellow)
                    }
                } else {
                    Text(label, style = PaperType.tag, color = colors.onYellow)
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "LATER",
                style = PaperType.tag,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            if (status is UpdateStatus.Failed) {
                Text(
                    "Download failed",
                    style = PaperType.small,
                    color = colors.muted,
                )
            }
        }
    }
}
