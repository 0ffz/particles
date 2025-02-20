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
import me.dvyy.particles.config.asMutableState
import me.dvyy.particles.dsl.ParticlesConfig
import me.dvyy.particles.ui.AppUI
import me.dvyy.particles.ui.helpers.FieldsWindow
import kotlin.time.Duration.Companion.seconds

fun <T, R> MutableStateValue<T>.map(mapping: (T) -> R): MutableStateValue<R> {
    return mutableStateOf(mapping(this.value)).also {
        onChange { oldValue, newValue -> it.set(mapping(newValue)) }
    }
}

class TextEditorWindow(
    ui: AppUI,
    val configRepository: ConfigRepository,
    val scope: CoroutineScope,
) : FieldsWindow("File editor", ui) {
    val consoleFont = MutableStateFlow(MsdfFont.DEFAULT_FONT)
    val consoleFontAsState = consoleFont.asMutableState(scope, MsdfFont.DEFAULT_FONT)
    val yamlKey = consoleFontAsState.map { TextAttributes(it, Color.ORANGE) }
    val yamlValue = consoleFontAsState.map { TextAttributes(it, Color.WHITE) }
    val yamlTag = consoleFontAsState.map { TextAttributes(it, Color.LIGHT_YELLOW) }
    val yamlComment = consoleFontAsState.map { TextAttributes(it, Color.DARK_GRAY) }
    private val lines = MutableStateFlow(listOf<String>())
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
                        value.split(" ").forEach {
                            if (it.startsWith("!")) add("$it " to yamlTag.value)
                            else add("$it " to yamlValue.value)
                        }
                    }
                )

            }.toMutableList()
        )
    }.asMutableState(scope, default = ListTextLineProvider(mutableListOf()))

    //    private val lines = mutableStateListOf<TextLine>().apply {
//        add(TextLine(listOf("hello world" to null)))
//    }
    init {
        scope.launch {
            configRepository.configLines.collect { lines ->
                this@TextEditorWindow.lines.update { lines.lines() }
            }
        }
        scope.launch {
            lines.debounce(3.seconds).collectLatest {
                configRepository.saveConfigLines(lines.value.joinToString("\n"))
            }
        }
        scope.launch {
            consoleFont.update { MsdfFont("assets/fonts/hack/font-hack-regular").getOrThrow().copy(sizePts = 20f) }
        }
    }
//val configText = FileSystemUtils.read(configRepository.configPath) ?: "{}"
//    configText.lines().forEach {
//        add(
//        )
//    }

    init {
        windowDockable.setFloatingBounds(width = Dp(500f), height = Dp(800f))
    }

    override fun UiScope.windowContent() {
        ReverseColumn(Grow.Std, Grow.Std) {
            Row(Grow.Std) {
                val config = decodedConfig.use()
                modifier.backgroundColor(
                    if (config.isSuccess) colors.backgroundVariant else (MdColor.RED tone 500).withAlpha(
                        0.1f
                    )
                )
                config.onSuccess {
                    Button("Reload from file") {
                        modifier.align(AlignmentX.End, AlignmentY.Bottom).margin(sizes.gap)
                        modifier.onClick {
                            val decoded = decodeConfigFromText(lines.value)
                            decodedConfig.set(decoded)
                            decoded.onSuccess {
                                configRepository.updateConfig(it)
                            }
                        }
                    }
                }
                config.onFailure {
                    Text("Failed to load config: ${it.message}") {
                        modifier.margin(sizes.gap)
                    }
                }
            }
            TextArea(
                formattedLines.use(),
//            hScrollbarModifier = { it.margin(start = sizes.gap, end = sizes.gap * 2f, bottom = sizes.gap) },
//            vScrollbarModifier = { it.margin(sizes.gap) }
            ) {
//            modifier.padding(horizontal = sizes.gap)
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
                            return caretPos
                        }
                    }
                })
//            modifier.width(Grow.Companion.Std).height(Grow.Companion.Std)
            }
        }
    }
}
