package com.aniwavestream.app.player

import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale

/**
 * Audio, subtitle, and video quality track models derived from Media3 [Tracks].
 */
@Immutable
data class SelectableTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val height: Int = 0,
    val bitrate: Int = Format.NO_VALUE
)

@Immutable
data class TrackSelectionUiState(
    val audioTracks: List<SelectableTrack> = emptyList(),
    val subtitleTracks: List<SelectableTrack> = emptyList(),
    val videoTracks: List<SelectableTrack> = emptyList(),
    val isAutoVideo: Boolean = true
) {
    val hasAudioOptions: Boolean get() = audioTracks.size > 1
    val hasSubtitleOptions: Boolean get() = subtitleTracks.isNotEmpty()
    val hasVideoOptions: Boolean get() = videoTracks.size > 1
}

/** Build the UI state from the current [Tracks] snapshot. */
fun Tracks.toTrackSelectionUiState(): TrackSelectionUiState {
    val audio = mutableListOf<SelectableTrack>()
    val subs = mutableListOf<SelectableTrack>()
    val video = mutableListOf<SelectableTrack>()
    var videoOverrideActive = false

    groups.forEachIndexed { gIndex, group ->
        for (t in 0 until group.length) {
            if (!group.isTrackSupported(t)) continue
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
                C.TRACK_TYPE_VIDEO -> {
                    if (group.isTrackSelected(t) && group.mediaTrackGroup.length > 1) {
                        // If only one track selected from multi-track group via override, not auto
                    }
                    video += SelectableTrack(
                        groupIndex = gIndex,
                        trackIndex = t,
                        label = format.prettyVideoLabel(),
                        language = null,
                        isSelected = group.isTrackSelected(t),
                        height = format.height,
                        bitrate = format.bitrate
                    )
                }
            }
        }
        if (group.type == C.TRACK_TYPE_VIDEO && group.isSelected) {
            val selectedCount = (0 until group.length).count { group.isTrackSelected(it) }
            if (selectedCount == 1 && group.length > 1) {
                videoOverrideActive = true
            }
        }
    }

    // Prefer unique heights (highest first) for quality picker
    val quality = video
        .filter { it.height > 0 || it.bitrate != Format.NO_VALUE }
        .sortedByDescending { it.height.takeIf { h -> h > 0 } ?: (it.bitrate / 1000) }
        .distinctBy { if (it.height > 0) it.height else it.bitrate }

    return TrackSelectionUiState(
        audioTracks = audio,
        subtitleTracks = subs,
        videoTracks = quality,
        isAutoVideo = !videoOverrideActive
    )
}

private fun Format.prettyAudioLabel(): String {
    val base = label ?: languageDisplayName() ?: "Audio"
    val channels = when (channelCount) {
        6 -> " · 5.1"
        2 -> " · Stereo"
        else -> ""
    }
    return base + channels
}

private fun Format.prettySubtitleLabel(): String =
    label ?: languageDisplayName() ?: "Subtitles"

private fun Format.prettyVideoLabel(): String {
    val h = height
    return when {
        h >= 2160 -> "4K · ${h}p"
        h >= 1440 -> "1440p"
        h >= 1080 -> "1080p"
        h >= 720 -> "720p"
        h >= 480 -> "480p"
        h >= 360 -> "360p"
        h > 0 -> "${h}p"
        bitrate != Format.NO_VALUE -> "${bitrate / 1000} kbps"
        else -> label ?: "Video"
    }
}

private fun Format.languageDisplayName(): String? =
    language?.let { lang ->
        runCatching { Locale(lang).displayLanguage.ifBlank { lang } }.getOrDefault(lang)
    }

/** Apply the user's audio/subtitle/video choice to the player. */
fun Player.selectTrack(track: SelectableTrack, tracks: Tracks) {
    val group = tracks.groups.getOrNull(track.groupIndex) ?: return
    val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setOverrideForType(override)
        .build()
}

/** Clear video overrides so adaptive ABR (Auto) can pick quality again. */
fun Player.clearVideoOverrides() {
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .build()
}

/** Turn subtitles off entirely. */
fun Player.disableSubtitles() {
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .build()
}

/** Re-enable the text track type after it was disabled. */
fun Player.enableSubtitles() {
    trackSelectionParameters = trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .build()
}
