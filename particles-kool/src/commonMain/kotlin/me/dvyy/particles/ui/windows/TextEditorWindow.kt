package me.dvyy.particles.ui.windows

import de.fabmax.kool.modules.ui2.DefaultTextEditorHandler
import de.fabmax.kool.modules.ui2.Dp
import de.fabmax.kool.modules.ui2.ListTextLineProvider
import de.fabmax.kool.modules.ui2.TextArea
import de.fabmax.kool.modules.ui2.TextAttributes
import de.fabmax.kool.modules.ui2.TextLine
import de.fabmax.kool.modules.ui2.UiScope
import de.fabmax.kool.modules.ui2.editorHandler
import de.fabmax.kool.modules.ui2.mutableStateListOf
import de.fabmax.kool.modules.ui2.mutableStateOf
import de.fabmax.kool.modules.ui2.remember
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MsdfFont
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.helpers.FieldsWindow

class TextEditorWindow(
    ui: AppUI
): FieldsWindow("Text editor", ui) {
    val editorAttributes = mutableStateOf(TextAttributes(MsdfFont.DEFAULT_FONT.copy(sizePts = 20f), Color.WHITE))
    private val lines = mutableStateListOf<TextLine>().apply {
        add(TextLine(listOf("hello world" to editorAttributes.value)))
//        add(TextLine(listOf("hello world" to null)))
    }

    init {
        windowDockable.setFloatingBounds(width = Dp(1200f), height = Dp(800f))
    }

    override fun UiScope.windowContent() {
        TextArea(
            ListTextLineProvider(lines),
//            hScrollbarModifier = { it.margin(start = sizes.gap, end = sizes.gap * 2f, bottom = sizes.gap) },
//            vScrollbarModifier = { it.margin(sizes.gap) }
        ) {
//            modifier.padding(horizontal = sizes.gap)
            // make text area selectable
            installDefaultSelectionHandler()
            // make text area editable
            modifier.editorHandler(remember {
                DefaultTextEditorHandler(lines).apply {
                    editAttribs = editorAttributes.value
                }
            })
//            modifier.width(Grow.Companion.Std).height(Grow.Companion.Std)
        }
    }
}
