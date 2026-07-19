            val deadline = System.currentTimeMillis() + 25_000L
            while (System.currentTimeMillis() < deadline) {
                // Guard every player read: if the player is released mid-poll
                // (episode switch / back nav), reading playbackState throws
                // IllegalStateException. Swallow it and bail instead of
                // propagating the exception up the coroutine.
                val ready = runCatching {
                    exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY
                }.getOrElse { false }
                if (ready) { ok = true; break }
                if (vm.error.value != null) break
                kotlinx.coroutines.delay(500)
            }