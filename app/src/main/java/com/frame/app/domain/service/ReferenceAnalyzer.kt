package com.frame.app.domain.service

import com.frame.app.domain.model.ReferenceProfile

/**
 * Ingests a reference photograph and extracts a [ReferenceProfile] describing
 * its composition (subject placement, size, angle, horizon, negative space,
 * style). Phase 5 feature; declared now so the composition engine and library
 * are designed against a stable target.
 *
 * The input is an opaque handle (e.g. a content URI string) so this domain
 * interface stays free of Android image types; the platform implementation
 * resolves and decodes it.
 */
interface ReferenceAnalyzer {

    /**
     * Analyze the image at [imageHandle] and produce a composition profile.
     *
     * @param imageHandle platform-resolvable reference to the image
     * @param displayName user-facing name for the resulting profile
     * @return the extracted profile, or null if no usable composition was found
     */
    suspend fun analyze(imageHandle: String, displayName: String): ReferenceProfile?
}
