package ui.composable.modal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import ui.composable.effect.CustomOutlinedTextFieldColors
import utils.getButtonColors
import config.GlobalInstance.config as gC

@Composable
fun SheetsPickerModal(spreadsheets: List<Pair<String, String>>, onFileSelected: (String) -> Unit, onCreateNew: (String) -> Unit, onDismiss: () -> Unit) {
    val dialogState = rememberDialogState(size = DpSize(1280.dp, 720.dp))
    val newSheetName = remember {mutableStateOf("")}
    val showCreateNewSection = remember {mutableStateOf(false)}
    val darkGray = gC.darkGray.value
    val middleGray = gC.middleGray.value
    val lightGray = gC.lightGray.value

    DialogWindow(onDismiss, dialogState, true, "", undecorated = true, resizable = true) {

        Column() {
            // Barre de titre
            WindowDraggableArea(Modifier.fillMaxWidth().height(50.dp).background(darkGray)) {Row(Modifier.fillMaxSize()) {ModalTitleBar(gC, "Sélecteur Google Sheets", onDismiss)}}
            Surface(Modifier.fillMaxSize(), color = middleGray) {
                Column(Modifier.fillMaxSize().padding(20.dp), Arrangement.spacedBy(20.dp)) {
                    // Liste des feuilles disponibles
                    if (spreadsheets.isNotEmpty() && !showCreateNewSection.value) {
                        Text("Feuilles disponibles :", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        SheetsGrid(spreadsheets, onFileSelected)
                        Button({showCreateNewSection.value = true}, colors = getButtonColors(middleGray, darkGray, lightGray)) {
                            Row(Modifier, Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
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
                        Text("Aucune feuille Google Sheets disponible", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        Text("Créer une nouvelle feuille :", style = MaterialTheme.typography.subtitle1, color = lightGray)
                        OutlinedTextField(newSheetName.value, {newSheetName.value = it}, Modifier.fillMaxWidth(), label = {Text("Nom de la feuille")}, colors = CustomOutlinedTextFieldColors())
                        Button({onCreateNew(newSheetName.value)}, Modifier.fillMaxWidth(), newSheetName.value.isNotBlank(), colors = getButtonColors(middleGray, darkGray, lightGray)) {Text("Créer")}
                    }
                    // Bouton d'annulation
                    Button(onDismiss, Modifier.align(Alignment.End), colors = getButtonColors(middleGray, darkGray, lightGray)) {Text("Annuler")}
                }
            }
        }
    }
}

@Composable
fun SheetCard(id: String, name: String, onSelect: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().height(120.dp).padding(10.dp).clip(RoundedCornerShape(10.dp)).clickable {onSelect(id)}, color = gC.darkGray.value.copy(0.3f), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxSize().padding(20.dp), Arrangement.Start, Alignment.CenterVertically) {
            Icon(Icons.Filled.Description, null, Modifier.size(25.dp), tint = gC.lightGray.value)
            Spacer(Modifier.width(10.dp))
            Text(name, color = gC.lightGray.value, fontSize = 15.sp, style = MaterialTheme.typography.h6, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ColumnScope.SheetsGrid(spreadsheets: List<Pair<String, String>>, onFileSelected: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(spreadsheets.size / 4 + if (spreadsheets.size % 4 > 0) 1 else 0) {rowIndex ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                val firstIndex = rowIndex * 4
                val secondIndex = firstIndex + 1
                val thirdIndex = firstIndex + 2
                val fourthIndex = firstIndex + 3
                Box(Modifier.weight(1f)) {if (firstIndex < spreadsheets.size) {val (id, name) = spreadsheets[firstIndex]; SheetCard(id, name, onFileSelected)}}
                Box(Modifier.weight(1f)) {if (secondIndex < spreadsheets.size) {val (id, name) = spreadsheets[secondIndex]; SheetCard(id, name, onFileSelected)}}
                Box(Modifier.weight(1f)) {if (thirdIndex < spreadsheets.size) {val (id, name) = spreadsheets[thirdIndex]; SheetCard(id, name, onFileSelected)}}
                Box(Modifier.weight(1f)) {if (fourthIndex < spreadsheets.size) {val (id, name) = spreadsheets[fourthIndex]; SheetCard(id, name, onFileSelected)}}
            }
        }
    }
}