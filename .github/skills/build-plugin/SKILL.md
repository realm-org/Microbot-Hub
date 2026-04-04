---
name: build-plugin
description: "Build one or more Microbot Hub plugins using Gradle. USE FOR: compiling plugin JARs, building specific plugins by name, building all plugins, troubleshooting build failures. DO NOT USE FOR: creating new plugins, editing plugin code, publishing releases."
---

# Build Plugin Skill

## Overview

Microbot Hub uses Gradle with dynamic plugin discovery. Each plugin under `src/main/java/net/runelite/client/plugins/microbot/<pluginname>/` that contains a `*Plugin.java` file is automatically discovered and gets its own source set, compile task, and shadow JAR.

## Build Commands

### Build a single plugin (fastest for iteration)

```bash
./gradlew build -PpluginList=<PluginClassName>
```

The `<PluginClassName>` is the Java class name **without** `.java`. Examples:

```bash
./gradlew build -PpluginList=GotrPlugin
./gradlew build -PpluginList=PestControlPlugin
./gradlew build -PpluginList=AutoMiningPlugin
```

### Build multiple specific plugins

Comma-separate the plugin names (no spaces):

```bash
./gradlew build -PpluginList=GotrPlugin,PestControlPlugin,AutoMiningPlugin
```

### Build all plugins

```bash
./gradlew clean build
```

This scans every directory under the plugins source path and builds all discovered plugins. Takes significantly longer than targeted builds.

### Other useful tasks

| Command | Purpose |
|---------|---------|
| `./gradlew generatePluginsJson` | Generate `plugins.json` metadata with SHA256 hashes (requires JDK 11) |
| `./gradlew copyPluginDocs` | Copy plugin `docs/` folders to `public/docs/` |
| `./gradlew test` | Run all tests (test classes can access all plugin source sets) |
| `./gradlew validateJdkVersion` | Verify the correct JDK version is active |

## Finding the plugin name

The plugin name used with `-PpluginList` is the Java class name of the main plugin file (the one with `@PluginDescriptor`), without the `.java` extension. To find it:

```bash
# List all discoverable plugin names
find src/main/java/net/runelite/client/plugins/microbot -name "*Plugin.java" -printf "%f\n" | sed 's/.java$//'
```

## Build output

- Plugin JARs are written to `build/libs/<PluginName>-<version>.jar`
- The version comes from the `static final String version` field in the Plugin class
- JARs are shadow JARs that include any dependencies listed in the plugin's `dependencies.txt`

## Troubleshooting

- **JDK version errors**: This project requires JDK 11 (Adoptium). Run `./gradlew validateJdkVersion`.
- **Plugin not found**: Ensure the directory contains a file matching `*Plugin.java` and the class has `@PluginDescriptor`.
- **Dependency issues**: Check `src/main/resources/net/runelite/client/plugins/microbot/<pluginname>/dependencies.txt` for correct Maven coordinates.
- **Client JAR missing**: The build auto-downloads the Microbot client. Override with `-PmicrobotClientVersion=<version>` or `-PmicrobotClientPath=/path/to/jar` for offline use.

## Procedure

When asked to build a plugin:

1. Identify the plugin class name (e.g., `GotrPlugin`, `PestControlPlugin`)
2. Run `./gradlew build -PpluginList=<PluginClassName>` in the terminal
3. Verify the build succeeds and report the result
4. If building multiple plugins, comma-separate them in a single `-PpluginList` argument