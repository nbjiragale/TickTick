package com.niranjan.ticktick.feature.taskeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.niranjan.ticktick.core.designsystem.AppSymbol

@Composable
internal fun AttachmentMenu(maxHeight: Dp, onSelect: (AttachmentSource) -> Unit, onDismiss: () -> Unit) {
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    Popup(popupPositionProvider = remember(margin) { MenuPosition(above = true, margin = margin) },
        onDismissRequest = onDismiss, properties = PopupProperties(focusable = false)) {
        val shape = RoundedCornerShape(16.dp)
        Column(Modifier.width(196.dp).heightIn(max = maxHeight).shadow(3.dp, shape).clip(shape)
            .background(Color.White).semantics { paneTitle = "Attachments" }
            .verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            MenuRow(AppSymbol.Camera, "Take Photo") { onSelect(AttachmentSource.TakePhoto) }
            MenuRow(AppSymbol.Image, "Choose Photo") { onSelect(AttachmentSource.ChoosePhoto) }
            MenuRow(AppSymbol.Microphone, "Records") { onSelect(AttachmentSource.Records) }
            MenuRow(AppSymbol.Folder, "File") { onSelect(AttachmentSource.File) }
            MenuRow(AppSymbol.Scan, "Scan Documents") { onSelect(AttachmentSource.ScanDocuments) }
        }
    }
}
