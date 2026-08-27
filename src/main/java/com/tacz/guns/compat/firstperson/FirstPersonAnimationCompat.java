package com.tacz.guns.compat.firstperson;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.renderer.item.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Makes generic first-person body/animation mods yield while TACZ owns the viewmodel.
 *
 * <p>The compatibility contract is one-way: ordinary items stay under the other mod's control,
 * while an animated TACZ/LRTactical {@link AnimateGeoItemRenderer} with a loaded model keeps its
 * authored gun/hand animation without a second arm rig.</p>
 *
 * <ul>
 *   <li>First-person Model / Not Enough Animations: reflection-only public API bridges. As of
 *       2026-08-21 they do not publish NeoForge 26.2 files, so those hooks stay dormant until a
 *       matching build is installed.</li>
 *   <li>Punchy: no public Java disable API. Optional {@code @Pseudo} mixins route TACZ viewmodels
 *       through Punchy's supported item-blacklist / yield path. See
 *       {@code com.tacz.guns.mixin.compat.punchy}.</li>
 * </ul>
 */
public final class FirstPersonAnimationCompat {
    private static final String FIRST_PERSON_MODEL = "firstperson";
    private static final String NOT_ENOUGH_ANIMATIONS = "notenoughanimations";

    private static boolean fpmRegistrationAttempted;
    private static Object fpmActivationHandler;
    private static boolean neaInstalled;

    private static boolean neaLookupAttempted;
    private static @Nullable Field neaInstanceField;
    private static @Nullable Field neaTransformerField;
    private static @Nullable Method neaRenderingFirstPersonArm;

    private FirstPersonAnimationCompat() {
    }

    public static void init() {
        if (ModList.get().isLoaded(FIRST_PERSON_MODEL)) {
            registerFirstPersonModelHandler();
        }
        neaInstalled = ModList.get().isLoaded(NOT_ENOUGH_ANIMATIONS);
        punchyLookupAttempted = true;
        punchyInstalled = ModList.get().isLoaded("punchy");
        if (punchyInstalled) {
            GunMod.LOGGER.info("Punchy detected; TACZ viewmodels use the blacklist/yield mixins");
        }
    }

    public static boolean isPunchyLoaded() {
        if (!punchyLookupAttempted) {
            punchyLookupAttempted = true;
            punchyInstalled = ModList.get().isLoaded("punchy");
        }
        return punchyInstalled;
    }

    /**
     * Punchy can rewrite the delayed-draw ModelView after TACZ submits the gun.
     * Premultiplying the submit-time ModelView then pins the ocular mask to a
     * different basis than the Iris HAND draw, so clipping and reticle
     * containment look like they are off even though the mask log is clean.
     */
    public static boolean shouldBakeSubmitModelViewIntoScopeMask() {
        return !isPunchyLoaded();
    }

    /** Returns the kept/main-hand stack that TACZ will actually draw this frame. */
    public static ItemStack getMainRenderStack(LocalPlayer player) {
        ItemStack kept = KeepingItemRenderer.getRenderer().getCurrentItem();
        return kept != null && !kept.isEmpty() ? kept : player.getMainHandItem();
    }

    /** True only when TACZ has both a custom renderer and a real model to submit. */
    public static boolean isTaczViewmodel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var renderer = BuiltinItemRendererRegistry.INSTANCE.get(stack.getItem());
        return renderer instanceof AnimateGeoItemRenderer<?, ?> animated && animated.getModel(stack) != null;
    }

    public static boolean shouldUseTaczRenderer(@Nullable LocalPlayer player) {
        return player != null && isTaczViewmodel(getMainRenderStack(player));
    }

    public static boolean shouldVanillaRenderArms() {
        return !shouldUseTaczRenderer(Minecraft.getInstance().player);
    }

    private static void registerFirstPersonModelHandler() {
        if (fpmRegistrationAttempted) {
            return;
        }
        fpmRegistrationAttempted = true;
        try {
            ClassLoader loader = FirstPersonAnimationCompat.class.getClassLoader();
            Class<?> handlerClass = Class.forName(
                    "dev.tr7zw.firstperson.api.ActivationHandler", false, loader);
            Class<?> apiClass = Class.forName(
                    "dev.tr7zw.firstperson.api.FirstPersonAPI", false, loader);

            fpmActivationHandler = Proxy.newProxyInstance(loader, new Class<?>[]{handlerClass},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "preventFirstperson" -> shouldUseTaczRenderer(Minecraft.getInstance().player);
                        case "toString" -> "TACZ first-person viewmodel guard";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == (args == null ? null : args[0]);
                        default -> null;
                    });
            // FPM 2.7.2 intentionally accepts Object and dispatches ActivationHandler internally.
            apiClass.getMethod("registerPlayerHandler", Object.class)
                    .invoke(null, fpmActivationHandler);
            GunMod.LOGGER.info("Enabled First-person Model handoff for TACZ animated items");
        } catch (ReflectiveOperationException | LinkageError e) {
            GunMod.LOGGER.warn("First-person Model is loaded, but its activation API could not be registered", e);
        }
    }

    /** Mirrors NEA's vanilla first-person-arm guard around TACZ's direct AvatarRenderer call. */
    public static void beginDirectArmRender() {
        setNeaFirstPersonArm(true);
    }

    public static void endDirectArmRender() {
        setNeaFirstPersonArm(false);
    }

    private static void setNeaFirstPersonArm(boolean rendering) {
        if (!neaInstalled) {
            return;
        }
        try {
            if (!neaLookupAttempted) {
                neaLookupAttempted = true;
                ClassLoader loader = FirstPersonAnimationCompat.class.getClassLoader();
                Class<?> loaderClass = Class.forName(
                        "dev.tr7zw.notenoughanimations.NEAnimationsLoader", false, loader);
                Class<?> transformerClass = Class.forName(
                        "dev.tr7zw.notenoughanimations.logic.PlayerTransformer", false, loader);
                neaInstanceField = loaderClass.getField("INSTANCE");
                neaTransformerField = loaderClass.getField("playerTransformer");
                neaRenderingFirstPersonArm = transformerClass.getMethod(
                        "renderingFirstPersonArm", boolean.class);
            }
            if (neaInstanceField == null || neaTransformerField == null
                    || neaRenderingFirstPersonArm == null) {
                return;
            }
            Object instance = neaInstanceField.get(null);
            if (instance == null) {
                return;
            }
            Object transformer = neaTransformerField.get(instance);
            if (transformer != null) {
                neaRenderingFirstPersonArm.invoke(transformer, rendering);
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            neaInstanceField = null;
            neaTransformerField = null;
            neaRenderingFirstPersonArm = null;
            if (rendering) {
                GunMod.LOGGER.warn("Not Enough Animations hand-render guard could not be bridged", e);
            }
        }
    }
}
