# Aniwave Stream

Crunchyroll-style **anime streaming Android app** — dark UI, orange accent, home rows, browse by genre, search, detail + episode list, ExoPlayer, My List, and continue watching.

**Repo:** https://github.com/lk5029716-boop/AniwaveStream

## Screenshots (UX map)

| Tab | What you get |
|-----|----------------|
| **Home** | Featured hero, Continue Watching, Trending, This Season, Top Rated |
| **Browse** | Genre chips + poster grid |
| **Search** | Debounced live search |
| **Detail** | Banner, score/meta, synopsis, My List, episode list |
| **Player** | Media3 ExoPlayer, prev/next episode, progress saved |
| **My List** | Saved titles (DataStore) |
| **Profile** | App info + legal note |

## Stack

- Kotlin, Jetpack Compose, Material 3  
- Navigation Compose, ViewModel, Coroutines / Flow  
- Retrofit + Kotlinx Serialization → [Jikan API v4](https://jikan.moe/) (MyAnimeList metadata)  
- Coil images, Media3 ExoPlayer, DataStore preferences  

## Important legal note

This is a **portfolio / educational demo**.

- **Metadata** comes from Jikan (public MAL API).  
- **Video** uses **public sample files** (e.g. Big Buck Bunny), **not** licensed anime.  
- Do **not** plug in pirate scrapers or unauthorized streams.  
- For a real product, connect a **licensed** catalog + CDN and comply with rights holders.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug+ / AGP 8.7).  
2. **File → Open** → this folder.  
3. Let Gradle sync (JDK 17).  
4. Run on an emulator or device (API 26+).

```bash
# CLI (if SDK + JDK configured)
./gradlew :app:assembleDebug
```

> Gradle wrapper JAR is standard; Android Studio will generate/fetch wrapper binaries on first open if needed.

## Project layout

```
app/src/main/java/com/aniwavestream/app/
  data/api          Jikan Retrofit client
  data/model        DTOs + domain models + demo streams
  data/repository   Catalog + My List / progress
  ui/theme          Crunchyroll-inspired colors
  ui/components     Hero, posters, rows
  ui/home|browse|search|detail|player|mylist|profile
  ui/navigation     Bottom tabs + detail/player routes
  viewmodel         Home + Library
```

## Roadmap ideas

- [ ] Real licensed backend (auth, HLS signed URLs, subtitles)  
- [ ] Offline downloads (Widevine)  
- [ ] Profiles / parental controls  
- [ ] Chromecast / PiP  
- [ ] Unit + UI tests  

## License

Demo code for learning. Anime artwork/metadata rights belong to their owners / MAL. Sample videos are third-party public demos.
