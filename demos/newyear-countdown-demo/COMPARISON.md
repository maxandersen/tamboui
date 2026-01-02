# Comparison: ny2026 (Python) vs newyear-countdown-demo (Java/TamboUI)

## Overview

Both projects are New Year countdown applications with fireworks animations, but they differ significantly in implementation language, framework, and features.

## Project Structure

### ny2026 (Python)
- **Location**: `/Users/manderse/code/tui/tamboui/ny2026/`
- **Language**: Python 3.13+
- **Files**:
  - `main.py` - Main application with fireworks simulation
  - `braille_canvas.py` - Custom braille canvas implementation
  - `demo.py` - Demonstration of braille canvas capabilities
  - `pyproject.toml` - Project configuration
  - `README.md` - Documentation

### newyear-countdown-demo (Java/TamboUI)
- **Location**: `/Users/manderse/code/tui/tamboui/tamboui/demos/newyear-countdown-demo/`
- **Language**: Java
- **Framework**: TamboUI
- **Files**:
  - `NewYearCountdownDemo.java` - Single-file application
  - `build.gradle.kts` - Build configuration

## Key Differences

### 1. **Rendering System**

**ny2026:**
- Custom `BrailleCanvas` class implemented from scratch
- Uses Unicode Braille patterns (U+2800-U+28FF)
- 2x4 dot pattern per character (8 dots per character)
- Direct pixel manipulation with bitwise operations
- Supports both 8-color palette and 24-bit RGB colors

**newyear-countdown-demo:**
- Uses TamboUI's built-in `Canvas` widget with `Marker.BRAILLE`
- Leverages TamboUI's rendering pipeline
- Uses TamboUI's `Context` API for drawing operations
- Built-in shape primitives (`Points`, `Line`)

### 2. **Graphics & Animation**

**ny2026:**
- 3D perspective projection with camera movement
- Particles have Z-coordinates for depth
- Camera moves forward through Z-space at 15 pixels/second
- More complex physics with 3D coordinates
- Launch trails stored and rendered
- 450-750 particles per explosion
- Particle lifetime: 1.8-2.5 seconds

**newyear-countdown-demo:**
- 2D simulation (no Z-axis)
- Simpler physics model
- 110-230 particles per explosion
- Particle lifetime: 0.9-2.1 seconds
- Rocket trails rendered as lines
- "Hiss" trail effects during rocket ascent

### 3. **Countdown Display**

**ny2026:**
- Large 7-segment style digits (22 lines tall)
- Uses ASCII block characters (`█`) for digits
- Renders countdown in center of screen
- Shows "2026" when midnight is reached (green color)

**newyear-countdown-demo:**
- Custom 7-segment font (7 lines tall)
- Uses block characters (`█`) and half-block (`░`) for colon
- Renders countdown at 72% of screen height
- Shows "HAPPY NEW YEAR 2026!" message at midnight
- Displays days remaining if > 0
- Shows current local time in corner

### 4. **Sound**

**ny2026:**
- Full audio system using `sounddevice` library
- Generates explosion sounds procedurally
- Stereo panning based on explosion position
- Low-pass filtered noise with rumble
- Multiple concurrent sounds (up to 8)
- Dedicated audio thread with callback-based mixing
- Optional dependency (gracefully degrades if unavailable)

**newyear-countdown-demo:**
- Terminal bell (BEL character, `\u0007`)
- Single "boom" sound per explosion
- No audio library dependencies
- Sound depends on terminal configuration

### 5. **User Interaction**

**ny2026:**
- Space bar: Launch single firework
- 'q' or Ctrl+C: Quit
- Cross-platform keyboard input (Windows/Unix)
- Raw terminal mode for non-blocking input
- Initial instruction panel using Rich library

**newyear-countdown-demo:**
- Space bar: Trigger fireworks show (6 seconds)
- 'q': Quit (via TamboUI's event system)
- Uses TamboUI's focus management and event handling
- Help text in bottom border

### 6. **Fireworks Behavior**

**ny2026:**
- Fireworks spawn automatically only after midnight
- Manual fireworks can be triggered anytime with Space
- Spawn interval: 0.2-0.8 seconds (after midnight)
- Random launch positions (20%-80% of screen width)
- Target explosion height: 10%-33% from top
- Physics-based launch velocity calculation

**newyear-countdown-demo:**
- Fireworks show triggered at midnight (18 seconds) or manually (6 seconds)
- Spawn interval: 0.25-0.8 seconds during show
- Random launch positions (2 to width-2)
- Fixed fuse time: 0.9-1.7 seconds
- Explodes at apex or after fuse

### 7. **Time Handling**

**ny2026:**
- Uses `datetime` and `zoneinfo` for timezone-aware calculations
- Countdown to 2026-01-01 00:00:00 local time
- Returns "2026" string when midnight reached

**newyear-countdown-demo:**
- Uses Java `ZonedDateTime` and `ZoneId`
- Countdown to next New Year (computed dynamically)
- Shows days, hours, minutes, seconds
- Displays current local time in corner

### 8. **Performance & Architecture**

**ny2026:**
- Target: 60 FPS
- Frame time limiting
- Batch point rendering
- Pre-generated stereo audio cache
- Custom terminal manipulation (alternate screen mode)
- Manual terminal size detection

**newyear-countdown-demo:**
- Target: ~20 FPS (50ms tick rate)
- Uses TamboUI's tick system
- Groups particles by color for efficient rendering
- Leverages TamboUI's terminal abstraction
- Automatic terminal size handling via TamboUI

### 9. **Dependencies**

**ny2026:**
- `rich>=14.2.0` - Terminal UI library
- `numpy>=2.4.0` - Numerical computations
- `sounddevice>=0.5.3` - Audio playback (optional)

**newyear-countdown-demo:**
- TamboUI Toolkit (internal dependency)
- TamboUI Core (internal dependency)
- TamboUI Widgets (internal dependency)
- TamboUI TUI (internal dependency)
- TamboUI JLine (internal dependency)
- Standard Java libraries only

### 10. **Code Organization**

**ny2026:**
- Multiple classes in single file (`main.py`)
  - `SoundManager`
  - `Particle`
  - `Firework`
- Separate `BrailleCanvas` module
- ~1159 lines in `main.py`
- ~200 lines in `braille_canvas.py`

**newyear-countdown-demo:**
- All code in single file (`NewYearCountdownDemo.java`)
- Nested classes:
  - `FireworksShow`
  - `Rocket`
  - `Particle`
  - `FxColor` (enum)
  - `SevenSegFont`
- ~621 lines total
- More compact, framework-assisted code

## Similarities

1. Both use Braille characters for high-resolution terminal graphics
2. Both implement particle-based fireworks with physics
3. Both show countdown to New Year
4. Both support manual firework triggering with Space bar
5. Both use 7-segment style fonts for countdown display
6. Both handle terminal cleanup on exit
7. Both support quitting with 'q'

## Feature Comparison Table

| Feature | ny2026 | newyear-countdown-demo |
|---------|--------|------------------------|
| 3D Perspective | ✅ Yes | ❌ No |
| Audio System | ✅ Procedural stereo | ⚠️ Terminal bell only |
| Particle Count | 450-750 | 110-230 |
| Frame Rate | 60 FPS | ~20 FPS |
| Days Display | ❌ No | ✅ Yes |
| Current Time | ❌ No | ✅ Yes |
| Framework | Custom | TamboUI |
| Language | Python | Java |
| Dependencies | 3 external | Framework only |
| Code Size | ~1360 lines | ~621 lines |

## Use Cases

**Choose ny2026 if:**
- You want more advanced graphics (3D perspective)
- You want real audio effects
- You prefer Python
- You want higher frame rates
- You want more particles/visual effects

**Choose newyear-countdown-demo if:**
- You want a simpler, cleaner implementation
- You're working with TamboUI/Java ecosystem
- You want framework integration
- You prefer fewer dependencies
- You want more informational display (days, current time)

