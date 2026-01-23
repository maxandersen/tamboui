# Testing Comparison: Textual vs TamboUI

This document compares the testing capabilities of Textual (Python) and TamboUI (Java), highlighting differences, advantages, and potential improvements.

## Overview

Both frameworks provide a "Pilot" testing system that allows programmatic interaction with TUI applications in headless mode. However, there are significant architectural differences that affect the testing experience.

## Key Differences

### 1. **Asynchronous vs Synchronous**

**Textual (Python):**
```python
async def test_keys():
    async with app.run_test() as pilot:
        await pilot.press("r")
        await pilot.pause()
```

**TamboUI (Java):**
```java
@Test
void testKeys() throws Exception {
    try (TestRunner test = TestRunner.runTest(handler, renderer)) {
        Pilot pilot = test.pilot();
        pilot.press('r');
        pilot.pause();
    }
}
```

**Analysis:**
- **Textual advantage:** Async/await provides natural flow control and better integration with Python's asyncio ecosystem
- **TamboUI advantage:** Synchronous code is simpler and more familiar to Java developers; no need to understand async/await
- **TamboUI note:** Uses threads internally, which is idiomatic for Java but requires careful synchronization

### 2. **Widget Selection**

**Textual:**
```python
await pilot.click("#red")  # CSS selector
await pilot.click(Button, offset=(0, -1))  # Widget type + offset
```

**TamboUI:**
```java
pilot.click(35, 10);  // Absolute coordinates only
```

**Analysis:**
- **Textual advantage:** CSS selectors and widget type matching make tests more maintainable and less brittle
- **TamboUI limitation:** Currently requires absolute coordinates, which breaks if layout changes
- **Recommendation for TamboUI:** Add widget selection capabilities:
  - Support for finding widgets by ID or type
  - Relative positioning (e.g., "click widget at offset (0, -1)")
  - This would require adding widget identification to the rendering system

### 3. **Key Pressing**

**Textual:**
```python
await pilot.press("r", "g", "b")  # Multiple keys as strings
await pilot.press("ctrl+c")  # Modifiers as string
```

**TamboUI:**
```java
pilot.press('r');  // Single character
pilot.press(KeyCode.ESCAPE);  // Special keys
pilot.press("r", "g", "b");  // Multiple keys (varargs)
pilot.press(KeyCode.C, KeyModifiers.CTRL);  // Modifiers
```

**Analysis:**
- **Textual advantage:** More concise string-based API; modifiers as part of string
- **TamboUI advantage:** Type-safe enum for key codes; explicit modifier handling
- **TamboUI note:** The varargs `press(String...)` method provides similar convenience

### 4. **Event Processing**

**Textual:**
```python
await pilot.pause()  # Waits for all pending messages to be processed
await pilot.pause(delay=0.1)  # Optional delay parameter
```

**TamboUI:**
```java
pilot.pause();  // Fixed 50ms delay
```

**Analysis:**
- **Textual advantage:** `_wait_for_screen()` intelligently waits for message processing completion, not just a fixed delay
- **TamboUI limitation:** Fixed delay may be too short or too long depending on system load
- **Recommendation for TamboUI:** Implement proper event synchronization:
  - Add a method to wait for event queue to be empty
  - Add configurable delay parameter
  - Consider using `CountDownLatch` or similar for proper synchronization

### 5. **Terminal Resizing**

**Textual:**
```python
async with app.run_test(size=(100, 50)) as pilot:
    await pilot.resize_terminal(120, 60)
```

**TamboUI:**
```java
try (TestRunner test = TestRunner.runTest(handler, renderer, new Size(100, 50))) {
    pilot.resize(120, 60);
}
```

**Analysis:**
- **Both:** Similar functionality, both support initial size and runtime resizing
- **TamboUI advantage:** Type-safe `Size` object instead of tuple

### 6. **Mouse Interactions**

**Textual:**
```python
await pilot.click("#button", offset=(0, -1), control=True)  # Modifier support
await pilot.double_click(Button)
await pilot.hover("#widget")
```

**TamboUI:**
```java
pilot.click(10, 5);  // Basic click
pilot.click(MouseButton.RIGHT, 10, 5);  // Right click
pilot.mouseMove(10, 5);  // Hover equivalent
// No double/triple click yet
```

**Analysis:**
- **Textual advantage:** Built-in double/triple click, modifier key support for clicks
- **TamboUI limitation:** Missing double/triple click, no modifier support for mouse
- **Recommendation for TamboUI:** Add:
  - `doubleClick()` and `tripleClick()` methods
  - Modifier key support for mouse events (Ctrl+click, Shift+click, etc.)

### 7. **Test Setup**

**Textual:**
```python
app = RGBApp()
async with app.run_test() as pilot:
    # Test code
```

**TamboUI:**
```java
RgbAppExample app = new RgbAppExample();
EventHandler handler = app.createHandler();
Renderer renderer = app.createRenderer();
try (TestRunner test = TestRunner.runTest(handler, renderer)) {
    // Test code
}
```

**Analysis:**
- **Textual advantage:** App is a first-class object; `run_test()` is a method on the app
- **TamboUI limitation:** Requires manual creation of handler and renderer
- **Recommendation for TamboUI:** Consider adding a higher-level abstraction:
  - A `TestableApp` interface that apps can implement
  - A convenience method that extracts handler/renderer from an app object
  - This would make the API more similar to Textual's

## What's Better in TamboUI

1. **Type Safety:** Java's type system provides compile-time safety for key codes, modifiers, and mouse buttons
2. **Explicit Control:** The separation of `EventHandler` and `Renderer` makes the architecture clear
3. **No Async Complexity:** Synchronous code is easier to understand for developers not familiar with async/await
4. **Size Objects:** Using `Size` objects instead of tuples is more type-safe

## Recommendations for TamboUI

### High Priority

1. **Widget Selection:** Add support for finding and clicking widgets by ID or type
   ```java
   pilot.click("#red-button");
   pilot.click(Button.class, offset(0, -1));
   ```

2. **Event Synchronization:** Replace fixed `pause()` with proper event queue waiting
   ```java
   pilot.pause();  // Wait for events to process
   pilot.pause(Duration.ofMillis(100));  // Optional delay
   ```

3. **Double/Triple Click:** Add convenience methods
   ```java
   pilot.doubleClick(10, 5);
   pilot.tripleClick(10, 5);
   ```

### Medium Priority

4. **Modifier Keys for Mouse:** Support Ctrl+click, Shift+click, etc.
   ```java
   pilot.click(10, 5, KeyModifiers.CTRL);
   ```

5. **Higher-Level App API:** Make testing apps easier
   ```java
   interface TestableApp {
       EventHandler handler();
       Renderer renderer();
   }
   
   // Then:
   try (TestRunner test = TestRunner.runTest(app)) {
       // ...
   }
   ```

6. **Hover Support:** Add explicit hover method (currently `mouseMove` exists but could be clearer)
   ```java
   pilot.hover(10, 5);
   ```

### Low Priority

7. **Scroll Wheel:** Already supported via `scrollUp()` and `scrollDown()`
8. **Drag Operations:** Already supported via `drag()`

## Example Comparison

### Textual Example
```python
async def test_rgb():
    app = RGBApp()
    async with app.run_test() as pilot:
        await pilot.press("r")
        assert app.screen.styles.background == Color.parse("red")
        await pilot.click("#green")
        assert app.screen.styles.background == Color.parse("green")
```

### TamboUI Example
```java
@Test
void testRgb() throws Exception {
    RgbAppExample app = new RgbAppExample();
    try (TestRunner test = TestRunner.runTest(
            app.createHandler(), app.createRenderer())) {
        Pilot pilot = test.pilot();
        pilot.press('r');
        pilot.pause();
        assertEquals(BackgroundColor.RED, app.getCurrentColor());
        pilot.click(35, 11);  // Green button coordinates
        pilot.pause();
        assertEquals(BackgroundColor.GREEN, app.getCurrentColor());
    }
}
```

## Conclusion

TamboUI's pilot system provides solid testing capabilities with good type safety and explicit control. The main areas for improvement are:

1. **Widget selection** (biggest gap)
2. **Event synchronization** (reliability)
3. **Convenience features** (double-click, modifiers)

The synchronous nature of TamboUI is both a strength (simplicity) and a limitation (less natural flow control), but it fits well with Java's threading model.
