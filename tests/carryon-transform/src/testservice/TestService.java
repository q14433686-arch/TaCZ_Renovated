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
    public IFeatureValidator getFeatureValidator() { return IFeatureValidator.ALLOW_ALL; }
    public IAdviceProvider getAdviceProvider() { return IAdviceProvider.GENERIC; }
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
