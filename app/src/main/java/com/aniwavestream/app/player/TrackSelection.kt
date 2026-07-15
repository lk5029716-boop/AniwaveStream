package com.aniwavestream.app.player

import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale

/**
 * Rule 10 — Audio track & subtitle management.
 * Immutable UI models describing the selectable audio / subtitle tracks exposed
 * by the player, derived from Media3 [Tracks]. The player applies a user's pick
 * by building a [TrackSelectionOverride] on the owning group.
 */
@Immutable
data class SelectableTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@Immutable
data class TrackSelectionUiState(
    val audioTracks: List<SelectableTrack> = emptyList(),
    val subtitleTracks: List<SelectableTrack> = emptyList()
) {
    val hasAudioOptions: Boolean get() = audioTracks.size > 1
    val hasSubtitleOptions: Boolean get() = subtitleTracks.isNotEmpty()
}

/** Build the UI state from the current [Tracks] snapshot. */
fun Tracks.toTrackSelectionUiState(): TrackSelectionUiState {
    val audio = mutableListOf<SelectableTrack>()
    val subs = mutableListOf<SelectableTrack>()
    groups.forEachIndexed { gIndex, group ->
        for (t in 0 until group.length) {
            val format = group.getTrackFormat(t)
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> audio += SelectableTrack(
                    groupIndex = gIndex,
                    trackIndex = t,
                    label = format.prettyAudioLabel(),
                    language = format.language,
                    isSelected = group.isTrackSelected(t)
                )
                C.TRACK_TYPE_TEXT -> subs += SelectableTrack(
                    groupIndex = gIndex,
                    trackIndex = t,
                    label = format.prettySubtitleLabel(),
                    language = format.language,
                    isSelected = group.isTrackSelected(t)
                )
            }
        }
    }
    return TrackSelectionUiState(audioTracks = audio, subtitleTracks = subs)
}

private fun Format.prettyAudioLabel(): String {
    val base = label ?: languageDisplayName() ?: "Audio"
    val channels = when (channelCount) {
        6 -> " 5.1"
        2 -> " Stereo"
        else -> ""
    }
    return base + channels
}

private fun Format.prettySubtitleLabel(): String =
    label ?: languageDisplayName() ?: "Subtitles"

private fun Format.languageDisplayName(): String? =
    language?.let { lang ->
        runCatching { Locale(lang).displayLanguage.ifBlank { lang } }.getOrDefault(lang)
    }

/** Apply the user's audio/subtitle choice to the player (Rule 10). */
fun Player.selectTrack(track: SelectableTrack, tracks: Tracks) {
    val group = tracks.groups.getOrNull(track.groupIndex) ?: return
    val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setOverrideForType(override)
        .build()
}

/** Turn subtitles off entirely. */
fun Player.disableSubtitles() {
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

/** Re-enable the text track type after it was disabled. */
fun Player.enableSubtitles() {
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .build()
}
