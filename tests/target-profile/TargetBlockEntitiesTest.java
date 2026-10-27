package com.tacz.guns.block.entity;

import com.mojang.serialization.Codec;
import com.tacz.guns.block.StatueBlock;
import com.tacz.guns.util.TargetOwnerProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Exercises the real block-entity methods against minimal stubs, not Minecraft codecs. */
public final class TargetBlockEntitiesTest {
    private static final BlockPos POS = new BlockPos(0, 64, 0);

    public static void main(String[] args) {
        targetOwnerRoundTripsAndClears();
        statueDropsOnceWithOldState();
        statueDoesNotDropOnClient();
        System.out.println("TargetBlockEntitiesTest: passed (stubbed world and codecs)");
    }

    private static void targetOwnerRoundTripsAndClears() {
        for (String name : new String[]{"Notch", "靶子 1", "a name longer than sixteen characters", "quote \" and slash \\"}) {
            TargetBlockEntity original = new TargetBlockEntity(POS, new BlockState());
            Level server = new Level(false);
            original.setLevel(server);
            original.setCustomName(Component.literal(name));
            original.setOwner(TargetOwnerProfiles.fromName(name));
            require(server.updates == 1, "owner update refreshes target after naming");
            Memory values = new Memory();
            original.saveAdditional(values);
            require(original.parentSaves == 1, "NeoForge superclass save");
            require(name.equals(values.data.get("owner_name")), "owner name written");

            TargetBlockEntity loaded = new TargetBlockEntity(POS, new BlockState());
            loaded.loadAdditional(values);
            require(loaded.parentLoads == 1, "NeoForge superclass load");
            require(loaded.getOwner() != null && loaded.getOwner().name().orElseThrow().equals(name), "owner read");
            require(loaded.getCustomName() != null && loaded.getCustomName().getString().equals(name), "custom name read");

            // Packet/chunk loads are complete snapshots; absent keys must not retain stale data.
            loaded.loadAdditional(new Memory());
            require(loaded.getOwner() == null && loaded.getCustomName() == null, "absent keys clear stale data");
        }

        Memory legacy = new Memory();
        legacy.data.put("owner_name", "legacy");
        legacy.data.put("owner_uuid", "not-a-uuid");
        legacy.data.put("CustomName", "{broken");
        TargetBlockEntity loaded = new TargetBlockEntity(POS, new BlockState());
        loaded.loadAdditional(legacy);
        require(loaded.getOwner().name().orElseThrow().equals("legacy"), "legacy flat owner");
        require(loaded.getCustomName().getString().equals("{broken"), "malformed name is contained");
    }

    private static void statueDropsOnceWithOldState() {
        BlockState oldState = new BlockState().setValue(StatueBlock.FACING, Direction.EAST);
        Level server = new Level(false);
        StatueBlockEntity statue = new StatueBlockEntity(POS, oldState);
        statue.setLevel(server);
        statue.setGun(new ItemStack("rifle"));
        // The live level now returns an empty state; removal must use the old state argument.
        statue.preRemoveSideEffects(POS, oldState);
        statue.preRemoveSideEffects(POS, oldState);
        require(server.drops.size() == 1, "one drop across repeated removal");
        require(server.drops.get(0).pos().equals(new BlockPos(1, 65, 0)), "old facing controls drop position");
        require(server.drops.get(0).item().equals("rifle"), "gun preserved");
        require(statue.getGunItem().isEmpty(), "gun cleared after drop");
        require(statue.parentRemovals == 2, "super removal called");

        StatueBlockEntity fallback = new StatueBlockEntity(POS, new BlockState());
        Level otherServer = new Level(false);
        fallback.setLevel(otherServer);
        fallback.setGun(new ItemStack("other"));
        fallback.preRemoveSideEffects(POS, new BlockState());
        require(otherServer.drops.get(0).pos().equals(new BlockPos(0, 65, -1)), "missing facing fallback");
        Memory values = new Memory();
        fallback.saveAdditional(values);
        fallback.loadAdditional(values);
        require(fallback.parentSaves == 1 && fallback.parentLoads == 1, "statue superclass persistence");
    }

    private static void statueDoesNotDropOnClient() {
        BlockState state = new BlockState().setValue(StatueBlock.FACING, Direction.SOUTH);
        Level client = new Level(true);
        StatueBlockEntity statue = new StatueBlockEntity(POS, state);
        statue.setLevel(client);
        statue.setGun(new ItemStack("rifle"));
        statue.preRemoveSideEffects(POS, state);
        require(client.drops.isEmpty(), "no client-side item entity");
        require(!statue.getGunItem().isEmpty(), "client does not clear server-owned stack");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Memory implements ValueInput, ValueOutput {
        private final Map<String, Object> data = new HashMap<>();
        @Override
        public String getStringOr(String key, String fallback) {
            Object value = data.get(key);
            return value instanceof String s ? s : fallback;
        }
        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> read(String key, Codec<T> codec) {
            return Optional.ofNullable((T) data.get(key));
        }
        @Override
        public void putString(String key, String value) { data.put(key, value); }
        @Override
        public <T> void store(String key, Codec<T> codec, T value) { data.put(key, value); }
    }
}
