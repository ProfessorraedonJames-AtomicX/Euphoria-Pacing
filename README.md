# Euphoria Pacing (AtomicX) - Forge 1.21.11 (61.2.4)   v0.5.0
Client-side dynamic performance governor and frame-pacing mod. Built on ForgeGradle 7, JDK 21.

    ./gradlew runClient
    ./gradlew build        # jar in build/libs

## Key Features & Optimizations
- **Intelligent Adaptive Quality Governor**:
  - Monitors frame times and smoothed FPS.
  - VSync & framerate limit aware: never gets trapped into permanent quality drops on 60Hz/75Hz displays.
  - Strict hysteresis prevents jitter and oscillation.
  - Progressively scales entity distance, tile entities, particles, weather, simulation, and render distance.
  - Anti-freeze cooldown prevents recursive chunk rebuilding spikes.
- **Adaptive Block Entity Distance Culling**:
  - Dynamically scales rendering distance for chests, signs, banners, and skulls.
  - Eliminates the notorious base and storage room FPS drops.
- **Smart Particle Culling & Burst Protection**:
  - Distance-culls ambient particles out of the player's view cone.
  - Caps per-tick burst queues to completely prevent explosion and firework lag spikes.
- **Elastic Chunk Mesh Upload Budgeting**:
  - Dynamically balances GPU buffer upload time across frames.
  - Scales budget when moving fast to eliminate invisible chunk holes while keeping 1% low FPS smooth.
- **Proximity-Aware Chunk Rebuilds**:
  - Instant synchronous rebuilds for blocks directly mined or placed by the player.
  - Smoothly offloads background world generation sections to background worker threads.


Config: `config/euphoriapacing-client.toml`
Video Settings: "Euphoria Pacing (AtomicX) - Performance" section with Target FPS slider, Auto Quality toggle, Block Entity Culling, and Upload Budgeting.
