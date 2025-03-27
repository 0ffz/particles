package me.dvyy.particles.ui.windows

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MdColor
import de.fabmax.kool.util.MsdfFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.dvyy.particles.config.ConfigRepository
import me.dvyy.particles.config.YamlHelpers
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.helpers.asMutableState
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.Icons
import me.dvyy.particles.ui.helpers.FieldsWindow
import me.dvyy.particles.ui.viewmodels.ParticlesViewModel
import kotlin.time.Duration.Companion.seconds

class TextEditorWindow(
    ui: AppUI,
    val configRepository: ConfigRepository,
    val viewModel: ParticlesViewModel,
    val scope: CoroutineScope,
) : FieldsWindow("Config file", ui, icon = Icons.fileCode) {
    val consoleFont = MutableStateFlow(MsdfFont.DEFAULT_FONT)
    val consoleFontAsState = consoleFont.asMutableState(scope, MsdfFont.DEFAULT_FONT)
    val yamlKey = consoleFontAsState.map { TextAttributes(it, Color.ORANGE) }
    val yamlValue = consoleFontAsState.map { TextAttributes(it, Color.WHITE) }
    val yamlTag = consoleFontAsState.map { TextAttributes(it, Color.LIGHT_YELLOW) }
    val yamlComment = consoleFontAsState.map { TextAttributes(it, Color.DARK_GRAY) }
    private val lines = MutableStateFlow(listOf<String>())
    val textChanged = mutableStateOf(false)
    val decodedConfig = lines.debounce(0.75.seconds).map {
        decodeConfigFromText(it)
    }.asMutableState(scope, default = Result.success(ParticlesConfig()))

    fun decodeConfigFromText(text: List<String>) = runCatching {
        YamlHelpers.yaml.decodeFromString(ParticlesConfig.serializer(), text.joinToString("\n"))
    }

    private val formattedLines = combine(lines, consoleFont) { lines, consoleFont ->
        if (lines.firstOrNull()
                ?.isEmpty() != false
        ) ListTextLineProvider(mutableListOf(TextLine(listOf("" to yamlKey.value))))
        else ListTextLineProvider(
            lines.map { line ->
                val splitAt = line.indexOf(":") + 1
                TextLine(
                    if (line.trimStart().startsWith("#")) listOf(line to yamlComment.value)
                    else buildList {
                        add(line.take(splitAt) to yamlKey.value)
                        val value = line.drop(splitAt)
                        val sections = value.split(" ")
                        sections.forEachIndexed { index, it ->
                            val text = if (index == sections.lastIndex) it else "$it "
                            if (it.startsWith("!")) add(text to yamlTag.value)
                            else add(text to yamlValue.value)
                        }
                    }
                )

            }.toMutableList()
        )
    }.asMutableState(scope, default = ListTextLineProvider(mutableListOf()))

    init {
        scope.launch {
            configRepository.configLines.collect { lines ->
                this@TextEditorWindow.lines.update { lines.lines() }
            }
        }
        scope.launch {
            lines.debounce(2.seconds).collectLatest {
                configRepository.saveConfigLines(lines.value.joinToString("\n"))
            }
        }
        scope.launch {
            consoleFont.update {
                AppFonts.loadAll()
                AppFonts.MONOSPACED.copy(sizePts = 20f)
            }
        }
        windowDockable.setFloatingBounds(width = Dp(500f), height = Dp(800f))
    }

    override fun UiScope.windowContent() {
        ReverseColumn(Grow.Std, Grow.Std) {
            Row(Grow.Std) {
                val config = decodedConfig.use()
                modifier.backgroundColor(
                    if (config.isSuccess) colors.backgroundVariant else (MdColor.RED tone 500).withAlpha(0.1f)
                )
                config.onSuccess {
                    Button("Reload from file") {
                        modifier.margin(sizes.gap)
                        modifier.onClick {
                            val decoded = decodeConfigFromText(lines.value)
                            decodedConfig.set(decoded)
                            decoded.onSuccess {
                                configRepository.saveConfigLines(lines.value.joinToString("\n"))
                                configRepository.updateConfig(it)
                                textChanged.set(false)
                                viewModel.restartSimulation()
                            }
                        }
                    }
                    if (textChanged.use()) {
                        Text("(Reload required)") {
                            modifier.alignY(AlignmentY.Center)
                        }
                    }
                }
                config.onFailure {
                    Text("Failed to load config: ${it.message}") {
                        modifier.margin(sizes.gap)
                    }
                }
            }
            TextArea(formattedLines.use()) {
                modifier.padding(sizes.smallGap)
                // make text area selectable
                installDefaultSelectionHandler()
                // make text area editable
                modifier.editorHandler(remember {
                    object : TextEditorHandler {
                        override fun insertText(
                            line: Int,
                            caret: Int,
                            insertion: String,
                            textAreaScope: TextAreaScope,
                        ): Vec2i {
                            return replaceText(line, line, caret, caret, insertion, textAreaScope)
                        }

                        override fun replaceText(
                            selectionStartLine: Int,
                            selectionEndLine: Int,
                            selectionStartChar: Int,
                            selectionEndChar: Int,
                            replacement: String,
                            textAreaScope: TextAreaScope,
                        ): Vec2i {
                            val edited = lines.value.toMutableList()
                            val replaceLines = replacement.lines()
                            val caretPos = Vec2i(
                                if (replaceLines.size > 1) replaceLines.last().length else selectionStartChar + replaceLines.last().length,
                                selectionStartLine + replaceLines.lastIndex
                            )
                            if (edited.isEmpty()) {
                                edited.add(replacement)
                            } else {
                                val start = edited[selectionStartLine].take(selectionStartChar)
                                val end = edited[selectionEndLine].drop(selectionEndChar)
                                (selectionStartLine..selectionEndLine).forEach {
                                    edited.removeAt(selectionStartLine)
                                }
                                replaceLines.forEachIndexed { i, line ->
                                    edited.add(
                                        selectionStartLine + i,
                                        buildString {
                                            if (i == 0) append(start)
                                            append(line)
                                            if (i == replaceLines.lastIndex) append(end)
                                        }
                                    )
                                }
                            }
                            lines.update { edited }
                            textChanged.set(true)
                            return caretPos
                        }
                    }
                })
            }
        }
    }
}
