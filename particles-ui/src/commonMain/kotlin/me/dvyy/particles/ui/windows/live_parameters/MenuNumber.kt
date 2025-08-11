package me.dvyy.particles.ui.windows.live_parameters

import androidx.compose.runtime.*
import de.fabmax.kool.modules.compose.LocalColors
import de.fabmax.kool.modules.compose.composables.layout.Column
import de.fabmax.kool.modules.compose.composables.layout.Row
import de.fabmax.kool.modules.compose.composables.rendering.Text
import de.fabmax.kool.modules.compose.composables.toolkit.Checkbox
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenu
import de.fabmax.kool.modules.compose.composables.toolkit.DropdownMenuItem
import de.fabmax.kool.modules.compose.modifiers.*
import de.fabmax.kool.modules.ui2.AlignmentY
import de.fabmax.kool.modules.ui2.RoundRectBackground
import de.fabmax.kool.modules.ui2.RoundRectBorder
import de.fabmax.kool.modules.ui2.dp

@Composable
fun MenuItem(
    name: String,
    content: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(name, Modifier.fillMaxWidth().alignY(AlignmentY.Center))
        content()
    }
}

@Composable
fun MenuNumber(
    name: String,
    value: Number,
    onValueChange: (Number) -> Unit,
) {
    MenuItem(name) {
        TextInputWithTooltip(value, onValueChange)
    }
}

@Composable
fun MenuCheckbox(
    name: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    MenuItem(name) {
        Checkbox(value, onValueChange)
    }
}

@Composable
inline fun <reified T : Enum<T>> MenuEnum(
    name: String,
    value: T,
    noinline onValueChange: (T) -> Unit,
) {
    MenuItem(name) {
        var expanded by remember { mutableStateOf(false) }
        val colors = LocalColors.current
        // TODO combobox component in kool
        Column {
            Text(
                value.name.lowercase().capitalize(), Modifier
                    .background(RoundRectBackground(colors.backgroundVariant, 4.dp))
                    .border(RoundRectBorder(colors.primaryVariant, 4.dp, 1.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clickable { expanded = true }
            )

            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                enumValues<T>().forEach { enumValue ->
                    val background = if (enumValue == value)
                        Modifier.backgroundColor(LocalColors.current.primaryVariant)
                    else Modifier
                    DropdownMenuItem(
                        Modifier.fillMaxWidth().then(background),
                        onClick = { onValueChange(enumValue); expanded = false },
                    ) { Text(enumValue.name.lowercase().capitalize()) }
                }
            }
        }
    }
}
