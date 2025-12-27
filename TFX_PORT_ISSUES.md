# TamboUI TFX Port - Known Issues

This document tracks issues discovered during the port of tachyonfx to tamboui-tfx that need to be addressed in the core TamboUI libraries.

## Issue: Text Cells Lose Background Color When Rendering

**Status**: ✅ Fixed  
**Severity**: Medium  
**Affects**: `tamboui-core`, `tamboui-widgets`

### Problem

When the `Paragraph` widget renders text using `Buffer.setLine()`, it creates new `Cell` objects that completely replace existing cells, causing text cells to lose their background color that was previously set via `Buffer.setStyle()`.

### Root Cause

1. `Paragraph.render()` calls `buffer.setStyle(area, style)` to set background colors for the entire area
2. Then it calls `buffer.setLine(x, y, line)` to render text
3. `setLine()` internally calls `setString()` which creates new `Cell(symbol, style)` objects
4. The new cells only have the style from the `Span`, which may not include the background color
5. This overwrites the background that was set by `setStyle()`

### Current Workaround

Manually set `.bg(color)` on all text spans before rendering:

```java
Text text = Text.from(
    Line.from("Hello").bg(DARK0_SOFT),
    Line.from("World").bg(DARK0_SOFT)
);
```

This is tedious and error-prone, and doesn't match the Rust ratatui behavior where text cells inherit the area's background color.

### Proposed Solutions

#### Option A: Patch Existing Cells in `setString()` (Recommended)

Modify `Buffer.setString()` to patch existing cell styles instead of replacing them:

```java
// In Buffer.java, change setString():
public int setString(int x, int y, String string, Style style) {
    // ... existing bounds checking ...
    
    if (col >= area.left()) {
        Cell existing = get(col, y);
        Cell newCell = existing.patchStyle(style).symbol(symbol);
        set(col, y, newCell);
    }
    
    // ... rest of method ...
}
```

**Pros**:

- Preserves background colors automatically
- Matches Rust ratatui behavior
- Minimal API change

**Cons**:

- Changes behavior of `setString()` (may affect other code)
- Need to ensure symbol is set correctly

#### Option B: Patch After `setLine()` in `Paragraph`

Modify `Paragraph.render()` to patch cell styles after setting lines:

```java
// In Paragraph.java, after buffer.setLine(x, y, line):
// Patch text cells with paragraph style to preserve background
for (int col = x; col < x + lineWidth && col < textArea.right(); col++) {
    Cell cell = buffer.get(col, y);
    if (!cell.isEmpty()) {
        buffer.set(col, y, cell.patchStyle(style));
    }
}
```

**Pros**:

- Isolated to Paragraph widget
- No changes to core Buffer API

**Cons**:

- More complex logic in Paragraph
- Need to track which cells were set by setLine

#### Option C: Add `setLineWithBaseStyle()` Method

Add a new method that accepts a base style to patch with:

```java
public int setLine(int x, int y, Line line, Style baseStyle) {
    int col = x;
    List<Span> spans = line.spans();
    for (Span span : spans) {
        Style combinedStyle = baseStyle.patch(span.style());
        col = setString(col, y, span.content(), combinedStyle);
    }
    return col;
}
```

**Pros**:

- Explicit control
- Backward compatible (old method still works)

**Cons**:

- More API surface
- Requires callers to pass base style

### Impact

This issue affects:
- All widgets that render text over styled backgrounds
- Effects that need to preserve background colors during transitions
- Any code that relies on text cells inheriting area background colors

### Related Files

- `tamboui-core/src/main/java/dev/tamboui/buffer/Buffer.java`
  - `setString()` method (line ~141)
  - `setSpan()` method (line ~175)
  - `setLine()` method (line ~188)

- `tamboui-widgets/src/main/java/dev/tamboui/widgets/paragraph/Paragraph.java`
  - `render()` method (line ~63)

### Test Case

```java
// Expected: Text cells should have DARK0_SOFT background
// Actual: Text cells have no background (or default)

Rect area = new Rect(0, 0, 20, 5);
Buffer buffer = Buffer.empty(area);

// Set background
buffer.setStyle(area, Style.EMPTY.bg(DARK0_SOFT));

// Render text
Text text = Text.from("Hello World");
Paragraph paragraph = Paragraph.builder().text(text).build();
paragraph.render(area, buffer);

// Check: buffer.get(0, 0).style().bg() should contain DARK0_SOFT
// Currently: buffer.get(0, 0).style().bg() is empty
```

### Resolution

**Fixed in**: `tamboui-core/src/main/java/dev/tamboui/buffer/Buffer.java`

Modified `Buffer.setString()` to patch existing cell styles instead of replacing them, matching ratatui.rs behavior:

```java
// Before: set(col, y, new Cell(symbol, style));
// After:
Cell existing = get(col, y);
Cell newCell = existing.patchStyle(style).symbol(symbol);
set(col, y, newCell);
```

This ensures that when text is rendered, it preserves background colors and other style attributes that were previously set via `Buffer.setStyle()` or widget styles.

**Impact**: All widgets using `setString()`, `setSpan()`, or `setLine()` now automatically preserve background colors. No changes needed to individual widgets.

### References

- Rust ratatui behavior: Text cells inherit background from area
- Issue discovered while porting `basic-effects` demo from Rust tachyonfx
- Fixed: `/tamboui-core/src/main/java/dev/tamboui/buffer/Buffer.java` (setString method)
