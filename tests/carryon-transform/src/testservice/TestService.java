package testservice;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.service.*;

public final class TestService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public String getName() { return "isolated-test"; }
    public boolean isValid() { return true; }
    public Phase getInitialPhase() { return Phase.DEFAULT; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return null; }
    public IClassTracker getClassTracker() { return null; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    // Fabric 的 Mixin 分支（NeoForge 26.2 依赖的那一支）在 IMixinService 里把
    // getAdviceProvider() 加成了抽象方法；上游 SpongePowered 0.8.7 没有它，所以这里
    // 不加 @Override —— 换回上游分支时这条只是多余方法，不会编译失败。
    // 接口文档要求「不得返回 null」，因此给 Mixin 自带的 GENERIC 实现。
    public IAdviceProvider getAdviceProvider() { return IAdviceProvider.GENERIC; }
    // 同一条分支加的第二个抽象方法：特性校验器。测试只做普通 @Inject/@ModifyVariable，
    // 用 Mixin 自带的「全允许」实现（接口注明不得返回 null）。
    public IFeatureValidator getFeatureValidator() { return IFeatureValidator.ALLOW_ALL; }
    public Collection<String> getPlatformAgents() { return List.of(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("isolated"); }
    public InputStream getResourceAsStream(String name) { return ClassLoader.getSystemResourceAsStream(name); }
    public URL[] getClassPath() { return new URL[0]; }
    public Class<?> findClass(String name) throws ClassNotFoundException { return Class.forName(name); }
    public Class<?> findClass(String name, boolean init) throws ClassNotFoundException { return Class.forName(name, init, getClass().getClassLoader()); }
    public Class<?> findAgentClass(String name, boolean init) throws ClassNotFoundException { return findClass(name, init); }
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException { return getClassNode(name, false, 0); }
    public ClassNode getClassNode(String name, boolean transform) throws ClassNotFoundException, IOException { return getClassNode(name, transform, 0); }
    public ClassNode getClassNode(String name, boolean transform, int flags) throws ClassNotFoundException, IOException {
        InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream == null) throw new ClassNotFoundException(name);
        try (stream) {
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, flags);
            return node;
        }
    }
    public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
}
