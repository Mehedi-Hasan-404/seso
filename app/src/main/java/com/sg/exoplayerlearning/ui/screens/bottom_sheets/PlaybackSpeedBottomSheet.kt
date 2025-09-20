package com.sg.exoplayerlearning.ui.screens.bottom_sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun PlaybackSpeedBottomSheet(
    currentSpeed: Float,
    closeSheet: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
) {
    val playbackSpeeds = listOf(0.5f, 1f, 1.5f, 1.75f, 2f)

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Playback Speed",
            modifier = Modifier,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlaybackSpeedSliderWithButtons(
            onSpeedChange = onSpeedSelected,
            speedSteps = playbackSpeeds,
            selectedSpeed = currentSpeed
        )

        Spacer(modifier = Modifier.height(16.dp))

        PlaybackSpeedsRow(
            playbackSpeeds = playbackSpeeds,
            clickAction = {
                onSpeedSelected(it)
                closeSheet()
            },
        )
    }
}

@Composable
fun PlaybackSpeedsRow(
    playbackSpeeds: List<Float>,
    clickAction: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {

        playbackSpeeds.forEach {
            PlaybackSpeedPhilItem(
                modifier = Modifier.weight(1f),
                text = it,
                clickAction = clickAction,
            )
        }
    }
}

@Composable
fun PlaybackSpeedPhilItem(
    modifier: Modifier = Modifier,
    text: Float,
    clickAction: (Float) -> Unit
) {
    Box(
        modifier = modifier
            .clickable {
                clickAction(text)
            }
    ) {

        Text(
            text = "$text" + "x",
            fontSize = 14.sp,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 6.dp, bottom = 6.dp, start = 14.dp, end = 14.dp)
        )

    }
}

@Composable
fun CircleButton(
    modifier: Modifier = Modifier,
    text: String,
    clickAction: () -> Unit
) {
    IconButton(
        onClick = {
            clickAction()
        },
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
    ) {

        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            modifier = modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun PlaybackSpeedSliderWithButtons(
    modifier: Modifier = Modifier,
    speedSteps: List<Float>,
    selectedSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row (
            modifier = Modifier
                .weight(0.2f),
        ) {
            CircleButton(
                text = "-",
                clickAction = {
                    val currentIndex = speedSteps.indexOf(selectedSpeed)
                    if (currentIndex > 0) {
                        speedSteps.getOrNull(currentIndex - 1)?.let {
                            onSpeedChange(it)
                        }
                    }
                }
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )
        }

        PlaybackSpeedSlider(
            selectedSpeed = selectedSpeed,
            speedSteps = speedSteps,
            onSpeedChange = onSpeedChange,
            modifier = Modifier
                .weight(0.6f),
        )

        Row (
            modifier = Modifier
                .weight(0.2f),
        ) {
            Spacer(modifier = Modifier.width(16.dp))

            CircleButton(
                text = "+",
            ) {
                val currentIndex = speedSteps.indexOf(selectedSpeed)
                if (currentIndex < speedSteps.size - 1) {
                    speedSteps.getOrNull(currentIndex + 1)?.let {
                        onSpeedChange(it)
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSlider(
    modifier: Modifier = Modifier,
    speedSteps: List<Float>,
    selectedSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val min = speedSteps.first()
    val max = speedSteps.last()
    val stepSize = 0.05f

    // Snap to nearest 0.05 step
    fun snapToStep(v: Float): Float {
        val steps = ((v - min) / stepSize).roundToInt()
        return (min + steps * stepSize).coerceIn(min, max)
    }

    var sliderValue by remember { mutableFloatStateOf(snapToStep(selectedSpeed)) }

    Slider(
        value = sliderValue,
        onValueChange = {
            sliderValue = snapToStep(it)
            onSpeedChange(sliderValue)
        },
        valueRange = min..max,
        onValueChangeFinished = {
            sliderValue = snapToStep(sliderValue)
            onSpeedChange(sliderValue)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp), // enough for larger thumb
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent, // we'll draw our own
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
            )
        },
        track = { sliderState ->
            val fraction = ((sliderValue - min) / (max - min)).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.Gray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(Color.White)
                )
            }
        }
    )

    LaunchedEffect(selectedSpeed) {
        sliderValue = snapToStep(selectedSpeed)
    }

}


@Composable
@Preview
fun PlaybackSpeedPhilsPreview() {
    PlaybackSpeedBottomSheet(
        currentSpeed = 1f,
        closeSheet = {},
        onSpeedSelected = {}
    )
}

@Composable
@Preview
fun PreviewCircleButtons() {

    Row(
        modifier = Modifier.wrapContentWidth()
    ) {
        CircleButton(
            text = "-",
        ) {

        }

        Spacer(modifier = Modifier.width(24.dp))

        CircleButton(
            text = "+",
        ) {

        }
    }

}
