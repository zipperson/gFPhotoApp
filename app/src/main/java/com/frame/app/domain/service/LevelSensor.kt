package com.frame.app.domain.service

import com.frame.app.domain.model.TiltState
import kotlinx.coroutines.flow.Flow

/**
 * Supplies device tilt (roll/pitch) for the level indicator. Abstracted so the
 * UI/ViewModel depend on a [Flow] of [TiltState] rather than Android sensors,
 * keeping the sensor plumbing swappable and the consumers testable with a fake.
 */
interface LevelSensor {

    /**
     * Cold stream of tilt updates. Collection starts listening to the device
     * sensor; cancellation unregisters it. Emissions are throttled to a UI-
     * friendly rate by the implementation.
     */
    val tilt: Flow<TiltState>
}
