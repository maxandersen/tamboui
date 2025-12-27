# TamboUI Missing Features

This document tracks features from ratatui.rs that are not yet implemented in TamboUI.

## Issue: Viewport::Inline Support

**Status**: 🔴 Not Implemented  
**Severity**: Low  
**Affects**: `tamboui-tui`, `tamboui-core`  
**Related**: `logo-demo` demo

### Problem

Ratatui.rs supports `Viewport::Inline(height)` which allows rendering TUI applications inline with the terminal output, using only a fixed number of lines instead of taking over the entire screen. TamboUI currently only supports fullscreen rendering via the alternate screen buffer.

### Expected Behavior (from ratatui.rs)

```rust
let terminal = ratatui::init_with_options(TerminalOptions {
    viewport: Viewport::Inline(3),
});
```

This should:
1. Not use the alternate screen buffer
2. Use only the specified number of lines (e.g., 3 lines)
3. Render below the current cursor position
4. Not clear the screen
5. Allow terminal output to appear before/after the TUI content

### Current Workaround

Disable alternate screen buffer in `TuiConfig`:

```java
TuiConfig config = TuiConfig.builder()
    .alternateScreen(false)
    .build();
```

However, this still uses the full terminal height instead of a fixed number of lines.

### Implementation Requirements

To properly implement `Viewport::Inline(height)`, we need:

1. **Add viewport configuration to `TuiConfig`**:
   ```java
   public enum Viewport {
       FULLSCREEN,  // Default - uses alternate screen
       INLINE(int height),  // Fixed height inline rendering
       FIXED(Rect area)  // Fixed area rendering
   }
   ```

2. **Update `Terminal` class** to:
   - Track viewport type and dimensions
   - Limit buffer size to viewport dimensions for inline mode
   - Position rendering below cursor for inline mode
   - Handle cursor position tracking for inline viewport

3. **Update `TuiRunner`** to:
   - Pass viewport configuration to terminal
   - Handle viewport-specific initialization

### References

- [ratatui Viewport documentation](https://docs.rs/ratatui/latest/ratatui/terminal/enum.Viewport.html)
- [ratatui logo example](https://github.com/ratatui/ratatui/blob/main/ratatui-widgets/examples/logo.rs)
- [ratatui inline example](https://github.com/ratatui/ratatui/blob/main/examples/apps/inline/src/main.rs)

### Related Files

- `demos/logo-demo/src/main/java/dev/tamboui/demo/LogoDemo.java` - Uses workaround
- `tamboui-tui/src/main/java/dev/tamboui/tui/TuiConfig.java` - Needs viewport support
- `tamboui-core/src/main/java/dev/tamboui/terminal/Terminal.java` - Needs viewport handling



