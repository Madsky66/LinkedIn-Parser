package ui.composable.effect

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import config.GlobalInstance.config as gC

@Composable
fun CustomOutlinedTextFieldColors(): TextFieldColors {
    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = gC.lightGray.value,
        focusedBorderColor = gC.lightGray.value.copy(0.25f),
        unfocusedBorderColor = gC.lightGray.value.copy(0.15f),
        focusedLabelColor = gC.lightGray.value.copy(0.5f),
        unfocusedLabelColor = gC.lightGray.value.copy(0.5f),
        placeholderColor = gC.lightGray.value.copy(0.25f)
    )
    return textFieldColors
}