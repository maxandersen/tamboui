# tamboui-rewrite

OpenRewrite recipes for TamboUI API migrations.

## Current recipes

### `dev.tamboui.rewrite.TambouiKeyEventBreakingApiMigration`
A conservative migration for the `KeyEvent.character()` API change:

- marks every `KeyEvent.character()` usage as a search result
- rewrites direct `event.character() == 'x'` comparisons to `event.isChar('x')`

## Build

```bash
./gradlew :tamboui-rewrite:test
./gradlew :tamboui-rewrite:publishToMavenLocal
```

## Notes

This is intentionally narrow. It handles the obvious low-risk patterns and leaves the weird cases for manual review instead of pretending to be clever and breaking code in production.
