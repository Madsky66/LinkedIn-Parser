package ui.composable.modal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.madsky.linkedinscraper.generated.resources.Res
import com.madsky.linkedinscraper.generated.resources.google_logo
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import config.GlobalInstance.config as gC

@Composable
fun SettingsModal(applicationScope: CoroutineScope, onDismiss: () -> Unit) {
    val dialogState = rememberDialogState(size = DpSize(500.dp, 500.dp))

    fun onLogOut() {
        gC.isLoggedIn.value = false
    }

    DialogWindow(onDismiss, dialogState, true, "Sélectionner une feuille Google Sheets", undecorated = true, resizable = true) {
        val darkGray = gC.darkGray.value
        val middleGray = gC.middleGray.value
        val lightGray = gC.lightGray.value

        Column {
            // Barre de titre
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {Row(Modifier.fillMaxSize()) {ModalTitleBar(gC, "Paramètres", onDismiss)}}

            Column(Modifier.fillMaxSize().background(middleGray).padding(20.dp).clip(RoundedCornerShape(10)).background(darkGray).padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                // Thème
                Row(Modifier.weight(0.4f).fillMaxWidth().clip(RoundedCornerShape(25, 25, 0, 0)).background(middleGray).padding(20.dp), Arrangement.Center, Alignment.CenterVertically) {DarkThemeSwitch()}
                Spacer(Modifier.height(10.dp))
                // Clé API Apollo
                Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(0, 0, 0, 0)).background(middleGray).padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text("Apollo.io > Admin settings > Integrations > API > API Keys", color = lightGray)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {ApolloKeyModule(applicationScope)}
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().border(BorderStroke(1.dp, darkGray)).padding(20.dp, 10.dp), Arrangement.SpaceBetween) {
                        val text = if (gC.apiKey.value.isBlank()) {"Aucune clé validée"} else {gC.apiKey.value}
                        Text("Clé actuelle : ", color = lightGray)
                        Text(text, color = if (gC.apiKey.value.isBlank()) {lightGray} else {Color.Green.copy(0.5f)})
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Compte Google
                Row(Modifier.weight(0.45f).fillMaxWidth().clip(RoundedCornerShape(0, 0, 25, 25)).background(middleGray).padding(20.dp), Arrangement.Center, Alignment.CenterVertically) {
                    Button({onLogOut()}, Modifier.fillMaxSize(), shape = RoundedCornerShape(100), border = BorderStroke(2.dp, lightGray), colors = ButtonDefaults.buttonColors(Color.Red), elevation = ButtonDefaults.elevation(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically){
                            Image(painterResource(Res.drawable.google_logo), "Google Logo", Modifier.size(35.dp))
                            Text("Se déconnecter du compte Google", color = Color.White)
                            Spacer(Modifier.width(35.dp))
                        }
                    }
                }
            }
        }
    }
}