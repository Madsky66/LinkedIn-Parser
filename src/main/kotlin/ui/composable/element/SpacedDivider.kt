package ui.composable.element

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SpacedDivider(modifier: Modifier = Modifier, direction: String, thickNess: Dp = 1.dp, firstSpacer: Dp, secondSpacer: Dp) {
    when (direction) {
        "horizontal" -> Spacer(Modifier.width(firstSpacer))
        "vertical" -> Spacer(Modifier.height(firstSpacer))
    }
    when (direction) {
        "horizontal" -> Divider(modifier.height(thickNess))
        "vertical" -> Divider(modifier.width(thickNess))
    }
    when (direction) {
        "horizontal" -> Spacer(Modifier.width(secondSpacer))
        "vertical" -> Spacer(Modifier.height(secondSpacer))
    }
}