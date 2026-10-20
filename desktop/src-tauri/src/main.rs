use sorafy_lib::build_app_state;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("==================================================");
    println!("  SoraFy Desktop Audio Engine v1.0.0");
    println!("  Cross-Platform Music Streaming for Linux & Windows");
    println!("  Architecture: Domain-Driven Design (Clean Architecture)");
    println!("  Palette: Obsidian Void, Priangan Emerald & Electric Lime");
    println!("==================================================");

    let state = build_app_state();

    println!("\n[1/3] Initializing Catalog Repository Adapter...");
    let trending = state.catalog.get_trending().await?;
    println!("-> Retrieved {} tracks from catalog port.", trending.len());
    for (idx, track) in trending.iter().enumerate().take(3) {
        println!(
            "   #{} \"{}\" by {} [{}]",
            idx + 1,
            track.title,
            track.artist,
            track.audio_format.codec
        );
    }

    println!("\n[2/3] Initializing LRCLIB Lyrics Sync Adapter...");
    if let Some(first) = trending.first() {
        println!("-> Probing synchronized lyrics for \"{}\"...", first.title);
        match state
            .lyrics
            .fetch_lyrics(&first.id, &first.title, &first.artist, Some(first.duration_secs))
            .await
        {
            Ok(Some(lrc)) => {
                println!("-> Found {} synchronized lyric lines.", lrc.synced_lines.len());
            }
            Ok(None) => {
                println!("-> No synced lyrics for this track (plain lyrics fallback).");
            }
            Err(e) => {
                println!("-> Lyrics probe note: {}", e);
            }
        }
    }

    println!("\n[3/3] Initializing Storage Adapter...");
    let history = state.library.get_history().await?;
    println!("-> Loaded {} history items from local cache.", history.len());

    println!("\n✓ SoraFy Desktop Core Engine is operational and ready for UI binding.\n");
    Ok(())
}
