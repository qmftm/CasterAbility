# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```
mvn clean package
```

The shaded JAR is output to `target/`. The default Maven goal is `clean package`, so `mvn` alone also works.

## Project Overview

**CasterAbility** is a PaperMC plugin (API 1.21, targeting 1.21.8) written in Kotlin that extends Skript with custom syntax. It hard-depends on the `Skript` plugin at runtime.

## Architecture

- `CasterAbility.kt` — main plugin class (`JavaPlugin`). Holds a static `instance` companion. On enable, calls `registerSkriptSyntax()` which registers all custom Skript elements with `Skript.register*()`.
- `src/main/kotlin/me/qmftm/casterability/skript/` — all custom Skript syntax lives here, organized by type:
  - `effect/` — Skript effects (extend `ch.njol.skript.lang.Effect`)
  - Add `condition/`, `expression/`, `event/` subdirectories as the plugin grows.

## Adding New Skript Syntax

1. Create a class in the appropriate `skript/<type>/` package extending the relevant Skript base class (`Effect`, `Condition`, `SimpleExpression`, etc.).
2. Register it in `CasterAbility.registerSkriptSyntax()` with the matching `Skript.registerEffect/registerCondition/registerExpression()` call.

## Dependencies

- **Paper API 1.21.8-R0.1-SNAPSHOT** — provided at runtime (do not shade)
- **Skript 2.14.3** — provided at runtime (do not shade)
- **kotlin-stdlib-jdk8 1.9.22** — shaded into the output JAR