package com.niranjan.ticktick.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.ticktick.core.designsystem.AppIcon
import com.niranjan.ticktick.core.designsystem.AppSymbol
import com.niranjan.ticktick.core.designsystem.TickTickColors
import com.niranjan.ticktick.domain.model.ListSymbol
import com.niranjan.ticktick.feature.tasks.TasksUiState

@Composable
fun NavigationDrawer(
    width: Dp,
    state: TasksUiState,
    onToday: () -> Unit,
    onList: (String) -> Unit,
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onAddList: () -> Unit,
    onManageLists: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(width).fillMaxHeight(),
        drawerShape = RectangleShape,
        drawerContainerColor = Color.White,
        drawerTonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(195.dp).background(Brush.verticalGradient(listOf(TickTickColors.DrawerTop, Color(0xFFF5F9FF), Color.White))))
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp).height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().background(TickTickColors.Orange, CircleShape), contentAlignment = Alignment.Center) {
                            Text("N", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-3).dp).size(17.dp)
                                .background(Color(0xFFA8AAAC), CircleShape).border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { AppIcon(AppSymbol.Crown, Modifier.size(12.dp), Color.White) }
                    }
                    Spacer(Modifier.width(19.dp))
                    Text("Niranjan Jiragale", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    DrawerIconButton(AppSymbol.Search, "Search tasks", onSearch)
                    DrawerIconButton(AppSymbol.Bell, "Reminders", onNotifications)
                    DrawerIconButton(AppSymbol.Settings, "Settings", onSettings)
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f)) {
                    item {
                        DrawerRow("Today", state.todayCount, AppSymbol.Calendar, TickTickColors.Orange, onToday, state.today.dayOfMonth.toString())
                    }
                    items(state.snapshot.lists, key = { it.id }) { list ->
                        val symbol = when (list.symbol) {
                            ListSymbol.Inbox -> AppSymbol.Inbox
                            ListSymbol.Work -> AppSymbol.Work
                            ListSymbol.Personal -> AppSymbol.Home
                            ListSymbol.Welcome, ListSymbol.Custom -> AppSymbol.List
                        }
                        val tint = when (list.symbol) {
                            ListSymbol.Inbox -> TickTickColors.Orange
                            ListSymbol.Work -> Color(0xFF935638)
                            ListSymbol.Personal -> Color(0xFFD69462)
                            ListSymbol.Welcome -> Color(0xFFE0B132)
                            ListSymbol.Custom -> TickTickColors.DueTime
                        }
                        DrawerRow(list.name, state.listCount(list.id), symbol, tint, { onList(list.id) }, emoji = if (list.symbol == ListSymbol.Welcome) "👋" else null)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.weight(1f).height(48.dp).clickable(role = Role.Button, onClick = onAddList), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { AppIcon(AppSymbol.AddList, Modifier.size(21.dp), Color(0xFF55575A)) }
                        Spacer(Modifier.width(8.dp))
                        Text("Add", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onManageLists, modifier = Modifier.size(40.dp).semantics { contentDescription = "Manage lists" }) {
                        AppIcon(AppSymbol.Manage, Modifier.size(23.dp), Color(0xFF55575A))
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerIconButton(symbol: AppSymbol, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp).semantics { contentDescription = label }) {
        AppIcon(symbol, Modifier.size(23.dp))
    }
}

@Composable
private fun DrawerRow(
    title: String,
    count: Int,
    symbol: AppSymbol,
    tint: Color,
    onClick: () -> Unit,
    day: String = "5",
    emoji: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).clickable(role = Role.Button, onClick = onClick).padding(start = 20.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (emoji != null) Text(emoji, fontSize = 17.sp) else AppIcon(symbol, Modifier.size(20.dp), tint, day)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (count > 0) Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = TickTickColors.SecondaryText)
    }
}
