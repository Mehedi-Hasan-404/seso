package com.sg.exoplayerlearning.ui.screens.bottom_sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ReplayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sg.exoplayerlearning.BottomSheetType
import com.sg.exoplayerlearning.models.ActionType
import com.sg.exoplayerlearning.models.PlayerAction

@Composable
fun SettingsBottomSheet(
    playerActions: (PlayerAction) -> Unit,
    itemClick: (BottomSheetType) -> Unit,
    closeSheet: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        item {
            SettingsItem(text = "Playback Speed", imageVector = Icons.Default.PlayCircleOutline) {
                itemClick(BottomSheetType.PLAYBACK_SPEED)
            }
        }

        item {
            SettingsItem(text = "Lock Screen", imageVector = Icons.Default.Lock) {
                playerActions(PlayerAction(ActionType.LOCK))
                closeSheet()
            }
        }
    }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
    itemClick: () -> Unit,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                itemClick()
            }
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = text
        )

        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.padding(12.dp)
        )

    }

}

@Composable
@Preview
fun SettingsBottomSheetPreview() {
    SettingsBottomSheet(
        playerActions = {},
        itemClick = {}
    ) {

    }
}