package ui.composable.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*


@Composable
fun EllipsisVisualTransformation(): VisualTransformation {
    val ellipsisVisualTransformation = VisualTransformation {text ->
        if (text.text.length > 30) {
            val displayText = "..." + text.text.takeLast(27)
            TransformedText(AnnotatedString(displayText), object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {return if (offset <= text.text.length - 27) 3 else offset - (text.text.length - 27) + 3}
                override fun transformedToOriginal(offset: Int): Int {return if (offset <= 3) 0 else offset - 3 + (text.text.length - 27)}
            })
        }
        else {TransformedText(AnnotatedString(text.text), OffsetMapping.Identity)}
    }
    return ellipsisVisualTransformation
}