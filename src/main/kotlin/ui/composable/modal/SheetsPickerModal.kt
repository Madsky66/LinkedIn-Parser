package ui.composable.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import ui.composable.effect.CustomOutlinedTextFieldColors
import utils.getButtonColors
import config.GlobalInstance.config as gC

@Composable
fun SheetsPickerModal(spreadsheets: List<Pair<String, String>>, onFileSelected: (String) -> Unit, onCreateNew: (String) -> Unit, onDismiss: () -> Unit) {
    val dialogState = rememberDialogState(size = DpSize(500.dp, 400.dp))
    val newSheetName = remember {mutableStateOf("")}
    val showCreateNewSection = remember {mutableStateOf(false)}
    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value
    val lightGray = gC.lightGray.value

    DialogWindow(onDismiss, dialogState, true, "Sélectionner une feuille Google Sheets", undecorated = true, resizable = true) {

        Column() {
            // Barre de titre
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {Row(Modifier.fillMaxSize()) {ModalTitleBar(gC, "Sélecteur Google Sheets", onDismiss)}}
            Surface(Modifier.fillMaxSize(), color = middleGray) {
                Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.spacedBy(20.dp)) {
                    // Titre
                    Text("Sélectionner une feuille Google Sheets", style = MaterialTheme.typography.h6, color = lightGray)
                    // Liste des feuilles disponibles
                    if (spreadsheets.isNotEmpty() && !showCreateNewSection.value) {
                        Text("Feuilles disponibles :", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        LazyColumn(Modifier.weight(1f).fillMaxWidth().border(1.dp, darkGray.copy(0.5f)).padding(8.dp)) {
                            items(spreadsheets.size) {index ->
                                val (id, name) = spreadsheets[index]
                                Row(Modifier.fillMaxWidth().clickable {onFileSelected(id)}.padding(8.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Description, null, tint = lightGray)
                                    Text(name, color = lightGray)
                                }
                                Divider(color = darkGray.copy(0.3f))
                            }
                        }
                        // Bouton pour créer une nouvelle feuille
                        Button({showCreateNewSection.value = true}, colors = getButtonColors(middleGray, darkGray, lightGray)) {
                            Row(Modifier, Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, null)
                                Text("Créer une nouvelle feuille")
                            }
                        }
                    }
                    else if (showCreateNewSection.value) {
                        // Section pour créer une nouvelle feuille
                        Text("Créer une nouvelle feuille :", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        OutlinedTextField(newSheetName.value, {newSheetName.value = it}, Modifier.fillMaxWidth(), label = {Text("Nom de la feuille")}, colors = CustomOutlinedTextFieldColors())
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            Button({showCreateNewSection.value = false}, Modifier.weight(1f), colors = getButtonColors(middleGray, darkGray, lightGray),) {Text("Retour")}
                            Button({onCreateNew(newSheetName.value)}, Modifier.weight(1f), newSheetName.value.isNotBlank(), colors = getButtonColors(middleGray, darkGray, lightGray),) {Text("Créer")}
                        }
                    }
                    else {
                        // Aucune feuille disponible
                        Text("Aucune feuille Google Sheets disponible", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        // Section pour créer une nouvelle feuille
                        Text("Créer une nouvelle feuille :", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        //
                        OutlinedTextField(newSheetName.value, {newSheetName.value = it}, Modifier.fillMaxWidth(), label = {Text("Nom de la feuille")}, colors = CustomOutlinedTextFieldColors())
                        //
                        Button({onCreateNew(newSheetName.value)}, Modifier.fillMaxWidth(), newSheetName.value.isNotBlank(), colors = getButtonColors(middleGray, darkGray, lightGray)) {Text("Créer")}
                    }
                    // Bouton d'annulation
                    Button(onDismiss, Modifier.align(Alignment.End), colors = getButtonColors(middleGray, darkGray, lightGray)) {Text("Annuler")}
                }
            }
        }
    }
}