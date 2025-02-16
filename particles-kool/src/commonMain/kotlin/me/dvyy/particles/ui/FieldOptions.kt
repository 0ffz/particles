package me.dvyy.particles.ui

import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.modules.ui2.docking.Dock
import de.fabmax.kool.modules.ui2.docking.UiDockable
import de.fabmax.kool.util.MdColor
import kotlin.jvm.JvmName

class FieldOptions(
    val resetPositions: () -> Unit,
    val load: () -> Unit,
    val save: () -> Unit,
    val state: FieldsState,
) {
    val windowDockable = UiDockable("Test")
    val colors = Colors.darkColors(
        background = MdColor.GREY tone 900
    )
    val dock = Dock("Dock").apply {
        dockingSurface.colors = colors
    }

    val windowSurface = WindowSurface(windowDockable) {
        var isMinimizedToTitle by remember(false)

        modifier.align(AlignmentX.Start, AlignmentY.Top)
        surface.colors = this@FieldOptions.colors
//        modifier.backgroundColor(MdColor.YELLOW tone 900)

        Box { modifier.width(180.dp) }

        Column(Grow.Std, Grow.Std) {
            TitleBar(
                windowDockable,
                isMinimizedToTitle = isMinimizedToTitle,
                onMinimizeAction = if (!isMinimizedToTitle) {
                    {
                        isMinimizedToTitle = true
                        windowDockable.setFloatingBounds(height = FitContent)
                    }
                } else null,
                onMaximizeAction = if (isMinimizedToTitle) {
                    { isMinimizedToTitle = false }
                } else null,
            )
            if (!isMinimizedToTitle) {
                ScrollArea(
                    withHorizontalScrollbar = false,
                    containerModifier = { it.background(null) }
                ) {
                    modifier.width(Grow.Std)
                    windowContent()
                }
            }
        }
    }

    fun UiScope.liveSlider(
        name: String, state: MutableStateValue<Float>,
        min: Float = 0f,
        max: Float = 1f,
    ): Unit = MenuSlider2(
        name,
        state.use(),
        min = min,
        max = max,
        onChange = { state.value = it },
    )

    @JvmName("liveSliderInt")
    fun UiScope.liveSlider(
        name: String, state: MutableStateValue<Int>,
        min: Float = 0f,
        max: Float = 1f,
    ): Unit = MenuSlider2(
        name,
        state.use().toFloat(),
        min = min,
        max = max,
        onChange = { state.value = it.toInt() },
    )

    fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
        liveSlider("Count", state.targetCount, max = 100_000f)
        liveSlider("Min grid size", state.minGridSize, max = 100f)
        liveSlider("dT", state.dT, max = 0.1f)
        liveSlider("Epsilon", state.epsilon, max = 1000f)
        LabeledSwitch("3d", state.threeDimensions)
//        Slider(value = fieldsState.epsilon.use(), max = 1_000f) {
//            modifier
//                .width(Grow.Std)
//                .onChange { fieldsState.epsilon.set(it) }
//        }
        Button("Reset positions") {
            modifier.onClick { resetPositions() }
        }
        Row(Grow.Std) {
            Button("Save") {
                modifier.onClick { save() }
            }
            Button("Load") {
                modifier.onClick { load() }
            }
        }
        Text("File content: " + state.epsilon.use()) { }
    }

    init {
        dock.addDockableSurface(windowDockable, windowSurface)
    }

//        setupUiScene(ClearColorFill(Scene.DEFAULT_CLEAR_COLOR))
//
//        addPanelSurface(colors = Colors.singleColorLight(MdColor.LIGHT_GREEN)) {
//            modifier
//                .size(400.dp, 300.dp)
//                .align(AlignmentX.Center, AlignmentY.Center)
//                .background(RoundRectBackground(colors.background, 16.dp))
//
//            var clickCount by remember(0)
//            Button("Click me!") {
//                modifier
//                    .alignX(AlignmentX.Center)
//                    .margin(sizes.largeGap * 4f)
//                    .padding(horizontal = sizes.largeGap, vertical = sizes.gap)
//                    .font(sizes.largeText)
//                    .onClick { clickCount++ }
//            }
//            Text("Button clicked $clickCount times") {
//                modifier
//                    .alignX(AlignmentX.Center)
//            }
//        }
}
