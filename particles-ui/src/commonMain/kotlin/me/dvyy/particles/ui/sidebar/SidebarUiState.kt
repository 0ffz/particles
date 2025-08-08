package me.dvyy.particles.ui.sidebar

import kotlinx.serialization.Serializable

@Serializable
class SidebarUiState(
    val isOpen: Boolean,
    val size: Double,
)
