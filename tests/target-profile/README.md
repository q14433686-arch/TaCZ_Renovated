# Target and statue unit tests

`targetProfileTest` and `targetBlockEntitiesTest` compile the production profile
factory and block entities against the minimal stubs in this directory, using an
isolated output directory and an empty classpath. The stubs never enter the mod JAR.
The tests exercise name handling, persistence wiring, stale-state clearing and
statue removal; they do **not** validate the actual Minecraft codecs, loader,
renderers, networking, profile service or in-game block lifecycle.

The tasks use the project's Java 25 toolchain (26.2 line; the 1.21.11 line uses 21), and
no project dependencies are needed because the stubs supply every referenced type:

```sh
./gradlew targetProfileTest targetBlockEntitiesTest
```

The tasks also run under `./gradlew check` and `./gradlew build`.
