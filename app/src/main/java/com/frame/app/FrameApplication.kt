package com.frame.app

import android.app.Application

/**
 * Application entry point. A deliberately thin composition root for Phase 1;
 * as services grow (camera, pose, storage, db) their singletons can be created
 * and wired here, keeping construction out of the UI.
 */
class FrameApplication : Application()
