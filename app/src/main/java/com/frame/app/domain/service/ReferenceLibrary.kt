package com.frame.app.domain.service

import com.frame.app.domain.model.ReferenceProfile
import kotlinx.coroutines.flow.Flow

/**
 * Persistence + organization for saved reference photos and their composition
 * profiles (Phase 6). Backed by Room in the platform layer; the Room scaffold
 * exists from Phase 1 but stores nothing until this feature is built out.
 */
interface ReferenceLibrary {

    /** Observe all saved references, newest first. */
    fun observeAll(): Flow<List<ReferenceProfile>>

    /** Observe references within a named collection/tag. */
    fun observeCollection(tag: String): Flow<List<ReferenceProfile>>

    suspend fun save(profile: ReferenceProfile)

    suspend fun get(id: String): ReferenceProfile?

    suspend fun delete(id: String)
}
