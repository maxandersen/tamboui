# Pilot Testing System - Dependency-Inverted Architecture

## Problem Statement

We need a testing system that:
1. Works with both `TuiRunner` and `ToolkitRunner`
2. Provides Textual-like API (`app.runTest()`)
3. Supports widget selection for ToolkitRunner
4. **Does NOT create cross-module dependencies** (tui shouldn't depend on toolkit)

## Solution: Test Fixtures + Interface-Based Design

Use Gradle's `java-test-fixtures` plugin to create a clean, dependency-inverted architecture.

## Architecture Overview

```
tamboui-tui (test fixtures)
├── Pilot (interface) - defines testing API
├── TestBackend - headless backend
├── TestRunner (interface) - defines test runner contract
└── Base testing infrastructure

tamboui-tui (main)
└── TuiTestRunner - implements TestRunner for TuiRunner
    └── TuiPilot - implements Pilot for TuiRunner

tamboui-toolkit (main)  
└── ToolkitTestRunner - implements TestRunner for ToolkitRunner
    └── ToolkitPilot - implements Pilot with widget selection
    └── ToolkitApp.runTest() - Textual-like API
```

**Key:** `tamboui-tui` test fixtures define the **interface**, each module provides its own **implementation**.

## Detailed Design

### 1. Test Fixtures in tamboui-tui

**Location:** `tamboui-tui/src/testFixtures/java/dev/tamboui/tui/pilot/`

#### 1.1 Pilot Interface

**File:** `tamboui-tui/src/testFixtures/java/dev/tamboui/tui/pilot/Pilot.java`

```java
/**
 * Interface for programmatically operating a TUI application during testing.
 * <p>
 * Implementations are provided by each module (TuiRunner, ToolkitRunner).
 */
public interface Pilot extends AutoCloseable {
    
    // Basic operations (all implementations)
    void press(KeyCode keyCode);
    void press(char c);
    void press(String... keys);
    void click(int x, int y);
    void mousePress(MouseButton button, int x, int y);
    void mouseRelease(MouseButton button, int x, int y);
    void mouseMove(int x, int y);
    void resize(int width, int height);
    void pause();
    void pause(Duration delay);
    void quit();
    
    // Widget selection (optional - throws UnsupportedOperationException if not supported)
    default void click(String elementId) {
        throw new UnsupportedOperationException("Widget selection not supported");
    }
    default void click(String elementId, int offsetX, int offsetY) {
        throw new UnsupportedOperationException("Widget selection not supported");
    }
    default Rect findElement(String elementId) {
        throw new UnsupportedOperationException("Widget selection not supported");
    }
    default boolean hasElement(String elementId) {
        return false;
    }
    
    // Enhanced features (optional)
    default void doubleClick(int x, int y) {
        click(x, y);
        pause();
        click(x, y);
    }
    default void doubleClick(String elementId) {
        click(elementId);
        pause();
        click(elementId);
    }
    default void tripleClick(int x, int y) {
        doubleClick(x, y);
        pause();
        click(x, y);
    }
    default void hover(int x, int y) {
        mouseMove(x, y);
        pause();
    }
    default void hover(String elementId) {
        Rect area = findElement(elementId);
        hover(area.centerX(), area.centerY());
    }
}
```

#### 1.2 TestRunner Interface

**File:** `tamboui-tui/src/testFixtures/java/dev/tamboui/tui/pilot/TestRunner.java`

```java
/**
 * Interface for test runners that provide a Pilot for testing.
 */
public interface TestRunner extends AutoCloseable {
    /**
     * Returns the pilot for controlling the test.
     */
    Pilot pilot();
    
    /**
     * Returns the underlying runner (TuiRunner or ToolkitRunner).
     */
    Object runner(); // Or use a common interface if we create one
}
```

#### 1.3 TestBackend

**File:** `tamboui-tui/src/testFixtures/java/dev/tamboui/tui/pilot/TestBackend.java`

Move `TestBackend` from `main` to `testFixtures` so it can be shared.

#### 1.4 ElementNotFoundException

**File:** `tamboui-tui/src/testFixtures/java/dev/tamboui/tui/pilot/ElementNotFoundException.java`

```java
public class ElementNotFoundException extends Exception {
    public ElementNotFoundException(String message) {
        super(message);
    }
}
```

### 2. TuiRunner Implementation

**Location:** `tamboui-tui/src/main/java/dev/tamboui/tui/pilot/`

#### 2.1 TuiPilot

**File:** `tamboui-tui/src/main/java/dev/tamboui/tui/pilot/TuiPilot.java`

```java
/**
 * Pilot implementation for TuiRunner applications.
 */
public final class TuiPilot implements Pilot {
    private final TuiRunner runner;
    private final TestBackend backend;
    
    TuiPilot(TuiRunner runner, TestBackend backend) {
        this.runner = runner;
        this.backend = backend;
    }
    
    // Implement all Pilot methods
    // Widget selection methods throw UnsupportedOperationException
}
```

#### 2.2 TuiTestRunner

**File:** `tamboui-tui/src/main/java/dev/tamboui/tui/pilot/TuiTestRunner.java`

```java
/**
 * Test runner for TuiRunner applications.
 */
public final class TuiTestRunner implements TestRunner {
    private final TuiRunner runner;
    private final TestBackend backend;
    private final TuiPilot pilot;
    private final Thread runnerThread;
    private final EventHandler handler;
    private final Renderer renderer;
    
    // Factory methods
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer) {
        return runTest(handler, renderer, new Size(80, 24));
    }
    
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer, Size size) {
        // Implementation
    }
    
    @Override
    public Pilot pilot() {
        return pilot;
    }
}
```

### 3. ToolkitRunner Implementation

**Location:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/app/`

#### 3.1 ToolkitPilot

**File:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/app/ToolkitPilot.java`

```java
/**
 * Pilot implementation for ToolkitRunner applications with widget selection.
 */
public final class ToolkitPilot implements Pilot {
    private final TuiRunner tuiRunner; // From ToolkitRunner
    private final TestBackend backend;
    private final FocusManager focusManager; // For widget selection
    
    ToolkitPilot(TuiRunner tuiRunner, TestBackend backend, FocusManager focusManager) {
        this.tuiRunner = tuiRunner;
        this.backend = backend;
        this.focusManager = focusManager;
    }
    
    // Implement all Pilot methods
    // Widget selection methods use focusManager.focusableAreas()
    
    @Override
    public void click(String elementId) throws ElementNotFoundException {
        Rect area = findElement(elementId);
        click(area.centerX(), area.centerY());
    }
    
    @Override
    public Rect findElement(String elementId) throws ElementNotFoundException {
        Map<String, Rect> areas = focusManager.focusableAreas();
        Rect area = areas.get(elementId);
        if (area == null) {
            throw new ElementNotFoundException("Element not found: " + elementId);
        }
        return area;
    }
    
    @Override
    public boolean hasElement(String elementId) {
        return focusManager.focusableAreas().containsKey(elementId);
    }
}
```

#### 3.2 ToolkitTestRunner

**File:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/app/ToolkitTestRunner.java`

```java
/**
 * Test runner for ToolkitRunner applications.
 */
public final class ToolkitTestRunner implements TestRunner {
    private final ToolkitRunner runner;
    private final TestBackend backend;
    private final ToolkitPilot pilot;
    private final Thread runnerThread;
    private final Supplier<Element> elementSupplier;
    
    // Factory methods
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier) {
        return runTest(elementSupplier, new Size(80, 24));
    }
    
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier, Size size) {
        // Implementation using TestBackend from test fixtures
    }
    
    @Override
    public Pilot pilot() {
        return pilot;
    }
}
```

#### 3.3 ToolkitApp.runTest()

**File:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/app/ToolkitApp.java`

```java
public TestRunner runTest() throws Exception {
    return runTest(new Size(80, 24));
}

public TestRunner runTest(Size size) throws Exception {
    return ToolkitTestRunner.runTest(this::render, size, configure());
}
```

### 4. FocusManager Access

**File:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/focus/FocusManager.java`

Add method to access registered areas:

```java
public Map<String, Rect> focusableAreas() {
    return Collections.unmodifiableMap(focusableAreas);
}
```

### 5. ToolkitRunner Factory

**File:** `tamboui-toolkit/src/main/java/dev/tamboui/toolkit/app/ToolkitRunner.java`

Add factory method to allow injecting TestBackend:

```java
public static ToolkitRunner create(Backend backend, TuiConfig config) throws Exception {
    TuiRunner tuiRunner = TuiRunner.create(backend, config);
    return new ToolkitRunner(tuiRunner);
}
```

## Module Dependencies

```
tamboui-tui
├── main
│   ├── depends on: tamboui-core
│   └── provides: TuiTestRunner, TuiPilot
└── testFixtures
    ├── depends on: tamboui-core
    └── provides: Pilot (interface), TestRunner (interface), TestBackend

tamboui-toolkit
├── main
│   ├── depends on: tamboui-core, tamboui-tui, tamboui-widgets, tamboui-css
│   ├── testImplementation: testFixtures(tamboui-tui)  ← Uses test fixtures!
│   └── provides: ToolkitTestRunner, ToolkitPilot, ToolkitApp.runTest()
```

**Key Point:** `tamboui-toolkit` uses `testFixtures(tamboui-tui)` to get the interfaces, but `tamboui-tui` main code doesn't depend on `tamboui-toolkit`.

## File Structure

```
tamboui-tui/
├── src/
│   ├── main/java/dev/tamboui/tui/pilot/
│   │   ├── TuiTestRunner.java (new - replaces TestRunner)
│   │   └── TuiPilot.java (new - implements Pilot)
│   └── testFixtures/java/dev/tamboui/tui/pilot/
│       ├── Pilot.java (interface - NEW)
│       ├── TestRunner.java (interface - NEW)
│       ├── TestBackend.java (moved from main)
│       └── ElementNotFoundException.java (NEW)

tamboui-toolkit/
└── src/main/java/dev/tamboui/toolkit/app/
    ├── ToolkitTestRunner.java (NEW)
    ├── ToolkitPilot.java (NEW)
    └── ToolkitApp.java (enhance - add runTest())
```

## API Examples

### TuiRunner Testing
```java
import dev.tamboui.tui.pilot.TuiTestRunner;

EventHandler handler = ...;
Renderer renderer = ...;

try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
    Pilot pilot = test.pilot();
    pilot.press('q');
    pilot.click(10, 5);
    // pilot.click("#button"); // Throws UnsupportedOperationException
}
```

### ToolkitRunner Testing
```java
import dev.tamboui.toolkit.app.ToolkitTestRunner;
import dev.tamboui.tui.pilot.Pilot; // From test fixtures

Supplier<Element> elementSupplier = () -> panel("Hello", text("World"));

try (ToolkitTestRunner test = ToolkitTestRunner.runTest(elementSupplier)) {
    Pilot pilot = test.pilot();
    pilot.press('q');
    pilot.click("#button-id");  // Widget selection works!
    pilot.doubleClick("#item");
}
```

### ToolkitApp Testing (Textual-like)
```java
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.tui.pilot.TestRunner; // From test fixtures

public class MyApp extends ToolkitApp {
    @Override
    protected Element render() {
        return panel("Title", button("Click me").id("button"));
    }
}

// In test:
MyApp app = new MyApp();
try (TestRunner test = app.runTest()) {  // Returns ToolkitTestRunner
    Pilot pilot = test.pilot();
    pilot.press('q');
    pilot.click("#button");  // Widget selection!
    pilot.doubleClick("#button");
}
```

## Benefits

1. **No Cross-Dependencies** - `tamboui-tui` doesn't depend on `tamboui-toolkit`
2. **Clean Interfaces** - Test fixtures define the contract
3. **Textual-like API** - `app.runTest()` works perfectly
4. **Widget Selection** - Available only where it makes sense
5. **Type Safety** - Interface ensures consistent API
6. **Extensible** - Easy to add more implementations later

## Implementation Steps

1. **Enable test fixtures in tamboui-tui**
   - Add `java-test-fixtures` plugin
   - Move `TestBackend` to test fixtures
   - Create `Pilot` and `TestRunner` interfaces

2. **Refactor TuiRunner testing**
   - Rename `TestRunner` → `TuiTestRunner`
   - Create `TuiPilot` implementing `Pilot`
   - Update existing tests

3. **Create ToolkitRunner testing**
   - Add `ToolkitRunner.create(Backend, TuiConfig)`
   - Create `ToolkitPilot` with widget selection
   - Create `ToolkitTestRunner`
   - Add `FocusManager.focusableAreas()` accessor

4. **Add ToolkitApp.runTest()**
   - Simple delegation to `ToolkitTestRunner.runTest()`

5. **Update examples and docs**

## Alternative: Single Module Approach

If we want to avoid test fixtures complexity, we could:

1. Keep everything in `tamboui-tui` but make `Pilot` an interface
2. `tamboui-toolkit` provides its own `ToolkitPilot` implementation
3. Use factory methods in each module

But test fixtures is cleaner and more idiomatic for shared test utilities.

## Recommendation

**Use test fixtures approach** because:
- ✅ Clean separation of concerns
- ✅ No cross-dependencies
- ✅ Standard Gradle pattern (already used in project)
- ✅ Interfaces define contract clearly
- ✅ Each module provides its own implementation

This gives us the best of both worlds: unified API through interfaces, but no dependency pollution.
