package org.bp.songbaobao.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bp.songbaobao.ui.navigation.Screen
import org.bp.songbaobao.ui.theme.*

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NavySoft, Ink)))
    ) {
        HorizontalDivider(thickness = 1.dp, color = PanelBorder)
        Row(modifier = Modifier.fillMaxWidth()) {
            Screen.barItems.forEach { screen ->
                val selected = currentRoute == screen.route
                TextButton(
                    onClick = { onSelect(screen.route) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            screen.icon,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(screen.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) GoldBright else TextDim,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
