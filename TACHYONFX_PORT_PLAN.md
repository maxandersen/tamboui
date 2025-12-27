# TachyonFX Port Plan for TamboUI

## Overview

This document outlines the plan for porting the Rust `tachyonfx` library to Java as a new `tamboui-fx` module in the TamboUI project. TachyonFX provides effects and animation capabilities for terminal UIs, allowing developers to create smooth transitions, color transformations, text animations, and complex visual effects.

## Java 8 Compatibility

**✅ YES, this port can be done while maintaining Java 8 compatibility.**

The `tamboui-fx` module will target **Java 8** to match TamboUI's library compatibility requirements. All modern Java features (sealed classes, records, switch expressions, pattern matching) will be replaced with Java 8-compatible alternatives:

- **Sealed classes** → Abstract class hierarchies with factory methods
- **Records** → Regular classes with constructors/getters
- **Switch expressions** → Traditional switch statements
- **Pattern matching** → `instanceof` checks with explicit casts

This approach is **not difficult** - it's a straightforward translation that maintains the same functionality while using Java 8 idioms. The code will be slightly more verbose but equally functional.

## Project Structure

### New Module: `tamboui-fx`

The new module will follow the existing TamboUI module structure:
- **Location**: `/tamboui-fx/`
- **Package**: `dev.tamboui.fx`
- **Dependencies**: 
  - `tamboui-core` (for Buffer, Cell, Rect, Style, Color, etc.)
  - **Java 8+** (must be compatible with Java 8, matching TamboUI library requirements)

## Architecture Overview

### Core Concepts

1. **Effects are stateful** - Created once, applied every frame
2. **Effects transform rendered content** - Applied after widgets render
3. **Effects compose** - Build complex animations from simple pieces

### Key Components to Port

#### 1. Core Types

##### Duration (`dev.tamboui.fx.Duration`)
- Port from `tachyonfx/src/duration.rs`
- Custom duration type (or use `java.time.Duration` with conversion)
- Support for milliseconds, seconds, etc.
- **Java Consideration**: May use `java.time.Duration` directly or create wrapper

##### Interpolation (`dev.tamboui.fx.Interpolation`)
- Port from `tachyonfx/src/interpolation.rs`
- Enum with all easing functions (Linear, QuadIn, QuadOut, CubicIn, etc.)
- Methods: `alpha(float)`, `flipped()`
- **Java Implementation**: Use enum with abstract method for alpha calculation

##### EffectTimer (`dev.tamboui.fx.EffectTimer`)
- Port from `tachyonfx/src/effect_timer.rs`
- Manages effect timing and progress
- Tracks elapsed time, duration, interpolation
- **Java Implementation**: Class with mutable state

#### 2. Shader System

##### Shader Interface (`dev.tamboui.fx.Shader`)
- Port from `tachyonfx/src/shader.rs`
- Core trait/interface for all effects
- Methods:
  - `name()`: String
  - `process(Duration, Buffer, Rect)`: Optional<Duration> (overflow)
  - `execute(Duration, Rect, Buffer)`: void
  - `done()`: boolean
  - `area()`: Optional<Rect>
  - `setArea(Rect)`: void
  - `filter(CellFilter)`: void
  - `timer()`: Optional<EffectTimer>
  - `clone()`: Shader (for Java, use Cloneable or factory pattern)
- **Java Implementation**: Interface with default methods where possible

##### Effect (`dev.tamboui.fx.Effect`)
- Port from `tachyonfx/src/effect.rs`
- Wrapper around Shader with builder pattern
- Methods:
  - `withArea(Rect)`: Effect
  - `withFilter(CellFilter)`: Effect
  - `withPattern(Pattern)`: Effect
  - `process(Duration, Buffer, Rect)`: Optional<Duration>
  - `done()`: boolean
- **Java Implementation**: Final class with fluent builder API

#### 3. Effect Manager

##### EffectManager (`dev.tamboui.fx.EffectManager`)
- Port from `tachyonfx/src/effect_manager.rs`
- Manages collection of active effects
- Methods:
  - `addEffect(Effect)`: void
  - `addUniqueEffect(K, Effect)`: void (K is key type)
  - `cancelUniqueEffect(K)`: void
  - `processEffects(Duration, Buffer, Rect)`: void
  - `isRunning()`: boolean
- **Java Implementation**: Generic class `EffectManager<K extends Comparable<K>>`

#### 4. Cell Iteration & Filtering

##### CellIterator (`dev.tamboui.fx.CellIterator`)
- Port from `tachyonfx/src/cell_iter.rs`
- Efficient iteration over buffer cells
- Supports filtering
- Methods:
  - `forEachCell(BiConsumer<Position, Cell>)`: void (preferred for performance)
  - Iterator interface for combinators
- **Java Implementation**: 
  - Implement `Iterable<CellEntry>` where `CellEntry` is (Position, Cell)
  - Provide `forEachCell` method for performance

##### CellFilter (`dev.tamboui.fx.CellFilter`)
- Port from `tachyonfx/src/cell_filter/`
- Complex filtering system with predicates
- Types: All, None, Text, FgColor, BgColor, Outer, Inner, AllOf, AnyOf, Not
- **Java Implementation**: Abstract class with factory methods and private concrete implementations (Java 8 compatible, instead of sealed classes)

##### FilterProcessor (`dev.tamboui.fx.FilterProcessor`)
- Processes cell filters efficiently
- **Java Implementation**: Internal class for optimization

#### 5. Color System

##### ColorSpace (`dev.tamboui.fx.ColorSpace`)
- Port from `tachyonfx/src/color_space.rs`
- Enum: Rgb, Hsl, Lab
- Color interpolation support
- **Java Implementation**: Enum with interpolation methods

##### Color Extensions
- Port color conversion utilities
- HSL/RGB conversion
- Color interpolation
- **Java Implementation**: Extension methods or utility class

##### ColorCache (`dev.tamboui.fx.ColorCache`)
- Port from `tachyonfx/src/color_cache.rs`
- LRU cache for color conversions
- **Java Implementation**: Use `LinkedHashMap` or similar

#### 6. Patterns

##### Pattern System (`dev.tamboui.fx.pattern`)
- Port from `tachyonfx/src/pattern/`
- Spatial patterns for effect distribution:
  - `RadialPattern`
  - `DiagonalPattern`
  - `CheckerboardPattern`
  - `SweepPattern`
  - `CoalescePattern`
  - `DissolvePattern`
- **Java Implementation**: Interface `Pattern` with implementations

#### 7. Built-in Effects (`dev.tamboui.fx.fx`)

Port all effects from `tachyonfx/src/fx/`:

##### Color Effects
- `fadeFrom(Color, Color, EffectTimer)`: Effect
- `fadeTo(Color, Color, EffectTimer)`: Effect
- `fadeFromFg(Color, EffectTimer)`: Effect
- `fadeToFg(Color, EffectTimer)`: Effect
- `hslShift(Color, EffectTimer)`: Effect
- `hslShiftFg(Color, EffectTimer)`: Effect

##### Text & Motion Effects
- `coalesce(EffectTimer)`: Effect
- `coalesceFrom(String, EffectTimer)`: Effect
- `dissolve(EffectTimer)`: Effect
- `dissolveTo(String, EffectTimer)`: Effect
- `evolve(String[], EffectTimer)`: Effect
- `slideIn(Direction, EffectTimer)`: Effect
- `slideOut(Direction, EffectTimer)`: Effect
- `sweepIn(Direction, Color, EffectTimer)`: Effect
- `sweepOut(Direction, Color, EffectTimer)`: Effect
- `explode(EffectTimer)`: Effect
- `expand(EffectTimer)`: Effect
- `stretch(Direction, EffectTimer)`: Effect

##### Control Effects
- `parallel(Effect[])`: Effect
- `sequence(Effect[])`: Effect
- `repeat(Effect, int)`: Effect
- `repeating(Effect)`: Effect
- `pingPong(Effect)`: Effect
- `delay(Duration, Effect)`: Effect
- `sleep(Duration)`: Effect
- `prolongStart(Duration, Effect)`: Effect
- `prolongEnd(Duration, Effect)`: Effect
- `freezeAt(float, Effect)`: Effect
- `remapAlpha(float, float, Effect)`: Effect
- `runOnce(Effect)`: Effect
- `neverComplete(Effect)`: Effect
- `timedNeverComplete(Duration, Effect)`: Effect
- `consumeTick()`: Effect
- `withDuration(Duration, Effect)`: Effect

##### Custom Effects
- `effectFn(State, EffectTimer, ShaderFunction<State>)`: Effect
- `effectFnBuf(State, EffectTimer, ShaderFunctionBuf<State>)`: Effect

**Java Implementation**: 
- Create `Fx` utility class with static factory methods
- Use builder pattern where appropriate
- Support method chaining

#### 8. Supporting Infrastructure

##### Math Utilities (`dev.tamboui.fx.Math`)
- Port from `tachyonfx/src/math.rs`
- Fast math operations (pow, sqrt, sin, cos, etc.)
- **Java Implementation**: Utility class with static methods

##### SimpleRng (`dev.tamboui.fx.SimpleRng`)
- Port from `tachyonfx/src/simple_rng.rs`
- Simple random number generator for deterministic effects
- **Java Implementation**: Class implementing deterministic RNG

##### LRU Cache (`dev.tamboui.fx.LruCache`)
- Port from `tachyonfx/src/lru_cache.rs`
- Generic LRU cache implementation
- **Java Implementation**: Generic class or use existing library

##### BitVec (`dev.tamboui.fx.BitVec`)
- Port from `tachyonfx/src/bitvec.rs`
- Efficient bit vector for tracking state
- **Java Implementation**: Use `BitSet` or custom implementation

## Integration with TamboUI

### Frame Integration

Add effect processing to `Frame` or `TuiRunner`:

```java
// Option 1: Add to Frame
public void applyEffects(EffectManager<?> effects, Duration delta) {
    effects.processEffects(delta, this.buffer, this.area);
}

// Option 2: Add to TuiRunner
// In render loop, after rendering widgets:
effects.processEffects(delta, frame.buffer(), frame.area());
```

### TuiRunner Integration

Modify `TuiRunner` to support effects:

```java
public class TuiRunner {
    private final EffectManager<String> effects = new EffectManager<>();
    
    public void run(EventHandler handler, Renderer renderer) throws Exception {
        Instant lastFrame = Instant.now();
        
        while (running.get()) {
            Duration delta = Duration.between(lastFrame, Instant.now());
            lastFrame = Instant.now();
            
            terminal.draw(frame -> {
                renderer.render(frame);
                effects.processEffects(delta, frame.buffer(), frame.area());
            });
            
            // ... event handling
        }
    }
}
```

## Implementation Phases

### Phase 1: Foundation (Core Types)
1. ✅ Create `tamboui-fx` module structure
2. ✅ Port `Duration` (or decide on Java Duration usage)
3. ✅ Port `Interpolation` enum with all easing functions
4. ✅ Port `EffectTimer`
5. ✅ Port `ColorSpace` and color utilities
6. ✅ Port `Math` utilities
7. ✅ Port `SimpleRng`

### Phase 2: Core Infrastructure
1. ✅ Port `CellIterator` and iteration system
2. ✅ Port `CellFilter` and filtering system
3. ✅ Port `Pattern` interface and basic patterns
4. ✅ Port `Shader` interface
5. ✅ Port `Effect` wrapper class
6. ✅ Port `EffectManager`

### Phase 3: Basic Effects
1. ✅ Port color effects (fade, hsl shift)
2. ✅ Port simple text effects (coalesce, dissolve)
3. ✅ Port basic motion effects (slide, sweep)
4. ✅ Create `Fx` utility class with factory methods

### Phase 4: Advanced Effects
1. ✅ Port complex effects (explode, expand, stretch)
2. ✅ Port control effects (parallel, sequence, repeat)
3. ✅ Port custom effect functions
4. ✅ Port all remaining effects

### Phase 5: Integration & Polish
1. ✅ Integrate with `TuiRunner`
2. ✅ Add demo applications
3. ✅ Write comprehensive tests
4. ✅ Documentation
5. ✅ Performance optimization

## Java 8 Compatibility

**CRITICAL**: The `tamboui-fx` module must be compatible with Java 8, matching TamboUI's library compatibility requirements.

### Java 8 Compatible Features (Available)
- ✅ **Lambdas & Method References**: Full support
- ✅ **Streams API**: Full support
- ✅ **Optional**: Full support
- ✅ **Default Methods in Interfaces**: Full support
- ✅ **Enums**: Full support
- ✅ **Generics**: Full support
- ✅ **Try-with-resources**: Full support
- ✅ **java.time.Duration**: Available (Java 8+)

### Java 8 Incompatible Features (Must Avoid)
- ❌ **Sealed Classes/Interfaces**: Java 17+ feature
  - **Solution**: Use abstract class hierarchy for `CellFilter`
  - Example: `abstract class CellFilter { ... }` with concrete subclasses
- ❌ **Records**: Java 14+ feature
  - **Solution**: Use regular classes with constructors/getters
  - Can use Lombok `@Value` if desired, but plain classes work fine
- ❌ **Switch Expressions**: Java 14+ feature
  - **Solution**: Use traditional switch statements or if-else chains
- ❌ **Pattern Matching**: Java 17+ feature
  - **Solution**: Use `instanceof` checks with explicit casts
- ❌ **Text Blocks**: Java 15+ feature
  - **Solution**: Use regular string concatenation (not critical)
- ❌ **var keyword**: Java 10+ feature
  - **Solution**: Use explicit types in library code (demos can use `var`)

### Java 8 Compatibility Patterns

#### CellFilter Implementation (Instead of Sealed Classes)
```java
// Java 8 compatible approach
public abstract class CellFilter {
    public static CellFilter all() { return new All(); }
    public static CellFilter none() { return new None(); }
    public static CellFilter text() { return new Text(); }
    public static CellFilter fgColor(Color color) { return new FgColor(color); }
    // ... more factory methods
    
    abstract boolean matches(Position pos, Cell cell);
    
    // Concrete implementations
    private static final class All extends CellFilter {
        boolean matches(Position pos, Cell cell) { return true; }
    }
    private static final class None extends CellFilter {
        boolean matches(Position pos, Cell cell) { return false; }
    }
    // ... more implementations
}
```

#### Data Classes (Instead of Records)
```java
// Java 8 compatible - simple data class
public final class CellEntry {
    private final Position position;
    private final Cell cell;
    
    public CellEntry(Position position, Cell cell) {
        this.position = position;
        this.cell = cell;
    }
    
    public Position position() { return position; }
    public Cell cell() { return cell; }
    
    // equals, hashCode, toString...
}
```

#### Pattern Matching Alternative
```java
// Java 8 compatible - use instanceof
if (filter instanceof FgColor) {
    FgColor fgColor = (FgColor) filter;
    // use fgColor
} else if (filter instanceof AllOf) {
    AllOf allOf = (AllOf) filter;
    // use allOf
}
```

### Memory Management
- **No RAII**: Use try-with-resources where appropriate
- **Garbage Collection**: Be mindful of object allocation in hot paths
- **Mutable vs Immutable**: Prefer immutable where possible, mutable for performance

### Type System
- **Generics**: Use generics for type safety (e.g., `EffectManager<K>`)
- **Abstract Class Hierarchies**: Use for closed type hierarchies (instead of sealed classes)
- **Regular Classes**: Use for data classes (instead of records)
- **Enums**: Use enums for `Interpolation`, `ColorSpace`, `Direction`

### Performance
- **Primitive Specialization**: Consider specialized versions for primitives
- **Object Pooling**: May be needed for high-frequency allocations
- **Method Inlining**: Use `final` methods where appropriate
- **Avoid Boxing**: Use primitive types in hot paths

### API Design
- **Builder Pattern**: Use for complex object construction
- **Fluent API**: Support method chaining
- **Static Factory Methods**: Use for common constructions
- **Default Methods**: Use interface default methods for optional behavior

### Thread Safety
- **Single-threaded by default**: TamboUI is single-threaded
- **Thread-safe variants**: Consider if needed for future multi-threading

## Testing Strategy

### Unit Tests
- Test each effect in isolation
- Test interpolation functions
- Test color conversions
- Test cell filtering
- Test pattern calculations

### Integration Tests
- Test effect composition
- Test effect manager lifecycle
- Test with actual Buffer rendering

### Demo Applications
- Basic effects demo
- Effect showcase
- Effect timeline visualization
- Interactive effect registry

## Documentation

### API Documentation
- Javadoc for all public APIs
- Examples for each effect
- Usage patterns and best practices

### User Guide
- Getting started guide
- Effect composition guide
- Performance tips
- Common patterns

## Dependencies

### Internal
- `tamboui-core`: Buffer, Cell, Rect, Style, Color, Position

### External
- None (keep it minimal, like tachyonfx)

## File Structure

```
tamboui-fx/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── java/
    │       └── dev/
    │           └── tamboui/
    │               └── fx/
    │                   ├── Duration.java
    │                   ├── Interpolation.java
    │                   ├── EffectTimer.java
    │                   ├── Shader.java
    │                   ├── Effect.java
    │                   ├── EffectManager.java
    │                   ├── CellIterator.java
    │                   ├── CellFilter.java
    │                   ├── ColorSpace.java
    │                   ├── Pattern.java
    │                   ├── Fx.java (factory methods)
    │                   ├── color/
    │                   │   ├── ColorCache.java
    │                   │   ├── ColorExtensions.java
    │                   │   └── ColorMapper.java
    │                   ├── fx/
    │                   │   ├── fade/
    │                   │   ├── dissolve/
    │                   │   ├── slide/
    │                   │   ├── containers/
    │                   │   └── ... (all effect implementations)
    │                   ├── pattern/
    │                   │   ├── RadialPattern.java
    │                   │   ├── DiagonalPattern.java
    │                   │   └── ...
    │                   └── util/
    │                       ├── Math.java
    │                       ├── SimpleRng.java
    │                       └── LruCache.java
    └── test/
        └── java/
            └── dev/
                └── tamboui/
                    └── fx/
                        └── ... (test files)
```

## Challenges & Solutions

### Challenge 1: Rust Traits → Java Interfaces
**Solution**: Use interfaces with default methods, abstract classes for shared implementation

### Challenge 2: Ownership & Borrowing
**Solution**: Java's GC handles this, but be careful with mutable state in effects

### Challenge 3: Zero-cost Abstractions
**Solution**: Use final methods, avoid unnecessary indirection, profile and optimize

### Challenge 4: Pattern Matching & Sealed Classes
**Solution**: Use abstract class hierarchies with factory methods and instanceof checks (Java 8 compatible)

### Challenge 5: Generic Constraints
**Solution**: Use bounded generics and interfaces

### Challenge 6: Performance
**Solution**: 
- Profile early and often
- Use primitive specializations where needed
- Minimize allocations in hot paths
- Consider object pooling for high-frequency effects

## Success Criteria

1. ✅ All core tachyonfx effects ported and working
2. ✅ Performance comparable to Rust version (within 2-3x is acceptable)
3. ✅ Clean, idiomatic Java API
4. ✅ Comprehensive test coverage (>80%)
5. ✅ Full documentation
6. ✅ Demo applications showcasing capabilities
7. ✅ Integration with TamboUI seamless

## Timeline Estimate

- **Phase 1**: 1-2 weeks
- **Phase 2**: 2-3 weeks
- **Phase 3**: 2-3 weeks
- **Phase 4**: 3-4 weeks
- **Phase 5**: 2-3 weeks

**Total**: ~10-15 weeks for complete port

## Next Steps

1. Review and approve this plan
2. Create `tamboui-fx` module structure
3. Begin Phase 1 implementation
4. Set up CI/CD for the new module
5. Create initial demo application

