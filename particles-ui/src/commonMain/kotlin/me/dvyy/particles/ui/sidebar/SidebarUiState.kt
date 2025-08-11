package me.dvyy.particles.ui.sidebar

import kotlinx.serialization.Serializable

@Serializable
data class SidebarUiState(
    val selectedTab: Int,
    val windowSize: Double,
)
