package com.fishme.sightmate.event

import com.fishme.sightmate.model.SightStyle

sealed interface Event {
    data object ControlClosed : Event
    data object RequestStyle : Event
    data class Move(val dx: Int, val dy: Int) : Event
    data class UpdateStyle(
        val size: Int? = null,
        val color: Int? = null,
        val alpha: Float? = null
    ) : Event
    data class StyleUpdated(val style: SightStyle) : Event
}

