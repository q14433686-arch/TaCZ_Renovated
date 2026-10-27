package net.minecraft.world.item.component;

/** Minimal stub: this test does not exercise Minecraft's actual profile or skin APIs. */
public final class ResolvableProfile {
    private final String name;

    private ResolvableProfile(String name) {
        this.name = name;
    }

    public static ResolvableProfile createUnresolved(String name) {
        if (name.equals("throws")) {
            throw new IllegalArgumentException("test failure");
        }
        return new ResolvableProfile(name);
    }

    public java.util.Optional<String> name() { return java.util.Optional.of(name); }

    public String testName() {
        return name;
    }
}
