package ui.composable.modal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import ui.composable.effect.CustomOutlinedTextFieldColors
import config.GlobalInstance.config as gC
import utils.*

@Composable
fun ApolloKeyModule(applicationScope: CoroutineScope) {
    var isApolloValidationLoading by remember {mutableStateOf(false)}
    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value
    val lightGray = gC.lightGray.value

    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        // Zone de texte
        OutlinedTextField(gC.pastedApiKey.value, {gC.pastedApiKey.value = it}, Modifier.clip(RectangleShape).weight(2f), textStyle = TextStyle.Default, label = {Text("Clé API Apollo...")}, maxLines = 1, colors = CustomOutlinedTextFieldColors())
        // Spacer
        Spacer(Modifier.width(10.dp))
        // Bouton de validation
        Button(
            onClick = {
                applicationScope.launch {
                    isApolloValidationLoading = true
                    gC.apiKey.value = gC.pastedApiKey.value
                    gC.consoleMessage.value = ConsoleMessage("⏳ Validation de la clé API par Apollo en cours...", ConsoleMessageType.INFO)
                    try {
                        // <--- Vérifier la validité de la clé ici
                        delay(500) // Simulation de la validation
                        gC.consoleMessage.value = ConsoleMessage("✅ La clé API a bien été validée par Apollo", ConsoleMessageType.SUCCESS)
                    }
                    catch (e: Exception) {gC.consoleMessage.value = ConsoleMessage("❌ Erreur lors de la validation de la clé API par Apollo : ${e.message}", ConsoleMessageType.ERROR)}
                    isApolloValidationLoading = false
                }
            },
            modifier = Modifier.padding(top = 8.dp).weight(0.75f).height(54.dp),
            enabled = gC.pastedApiKey.value.isNotBlank(),
            elevation = ButtonDefaults.elevation(10.dp),
            shape = RoundedCornerShape(0, 100, 100, 0),
            colors = getButtonColors(middleGray, darkGray, lightGray)
        ) {
            if (!isApolloValidationLoading) {Icon(Icons.AutoMirrored.Filled.Send, "")}
            else {CircularProgressIndicator(Modifier.align(Alignment.CenterVertically), lightGray, 5.dp)}
        }
    }
    Spacer(Modifier.height(10.dp))
}