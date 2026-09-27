package com.niranjan.ticktick.feature.reminders

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors

/** A compact, non-modal acknowledgement; its two-second lifetime is owned by the ViewModel. */
@Composable
internal fun SnoozeConfirmationCard(confirmation: SnoozeConfirmation) {
    key(confirmation.id) {
        Dialog(onDismissRequest = {}, properties = DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false,
        )) {
            // A separate window keeps the acknowledgement above editors and queued reminder dialogs.
            // It must not dim, steal keyboard focus, or intercept touches during its short lifetime.
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                window?.setDimAmount(0f)
                window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
            Surface(Modifier.widthIn(max = (LocalConfiguration.current.screenWidthDp.dp - 48.dp).coerceIn(1.dp, 300.dp))
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 6.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppSymbol.Check, Modifier.size(18.dp), TickTickColors.Accent)
                    Text(confirmation.message, style = MaterialTheme.typography.bodyMedium, color = TickTickColors.Text)
                }
            }
        }
    }
}
