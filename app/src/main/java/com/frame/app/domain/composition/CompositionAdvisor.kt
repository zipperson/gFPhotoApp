package com.frame.app.domain.composition

import com.frame.app.domain.model.AspectCheck
import com.frame.app.domain.model.SubjectFrame
import com.frame.app.domain.model.TiltState

/**
 * Turns failing [AspectCheck]s into concrete, prioritized, user-facing
 * suggestions ("Move slightly left", "Leave more space above the subject").
 *
 * Task 2 ships this as a stub returning no suggestions, so the engine builds and
 * scores on its own; Task 3 implements the real guidance mapping.
 */
open class CompositionAdvisor {
    open fun suggest(
        checks: List<AspectCheck>,
        subject: SubjectFrame,
        tilt: TiltState,
    ): List<String> = emptyList()
}
