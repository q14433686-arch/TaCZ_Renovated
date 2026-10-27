# Target and statue unit tests

`targetProfileTest` and `targetBlockEntitiesTest` compile the production profile
factory and block entities against the minimal stubs in this directory, using an
isolated output directory and an empty classpath. The stubs never enter the mod JAR.
The tests exercise name handling, persistence wiring, stale-state clearing and
statue removal; they do **not** validate the actual Minecraft codecs, loader,
renderers, networking, profile service or in-game block lifecycle.

Run with a full JDK 25 and the normal project dependencies:

```sh
./gradlew targetProfileTest targetBlockEntitiesTest
```

The tasks also run under `./gradlew check` and `./gradlew build`.
