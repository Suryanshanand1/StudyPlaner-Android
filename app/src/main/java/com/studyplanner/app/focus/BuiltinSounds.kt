package com.studyplanner.app.focus

import com.studyplanner.app.R

/**
 * Focus sounds that ship with the app. They live in res/raw and play offline like an
 * imported file; the user can still import their own audio instead.
 */
data class BuiltinSound(
    val id: String,
    val label: String,
    val resId: Int,
)

val builtinSounds: List<BuiltinSound> = listOf(
    BuiltinSound("nature", "Nature with melody", R.raw.focus_nature),
    BuiltinSound("rain_music", "Rain with calm music", R.raw.focus_rain_music),
    BuiltinSound("rain", "Rain", R.raw.focus_rain),
)

fun builtinSoundById(id: String?): BuiltinSound? =
    if (id == null) null else builtinSounds.firstOrNull { it.id == id }
