package com.tkisor.nekojs.core.fs;

import java.util.Set;
import java.util.function.Predicate;

public class ClassFilter implements Predicate<String> {

    public static final ClassFilter INSTANCE = new ClassFilter();

    public static volatile boolean allowThreads = false;
    public static volatile boolean allowReflection = false;
    public static volatile boolean allowAsm = false;

    private static final Set<String> THREAD_GROUP = Set.of("java.lang.Thread", "java.lang.ThreadGroup");
    private static final Set<String> REFLECT_GROUP = Set.of("java.lang.reflect", "java.lang.invoke.MethodHandles");
    private static final Set<String> ASM_GROUP = Set.of("org.objectweb.asm", "org.spongepowered.asm");
    private static final Set<String> GENERAL_BLACKLIST = Set.of(
            "java.lang.Runtime", "java.lang.Process", "java.lang.ProcessBuilder",
            "java.lang.ClassLoader", "java.lang.System",
            "java.io", "java.nio", "java.net", "java.util.jar", "java.util.zip",
            "sun", "com.sun",
            "io.netty", "org.openjdk.nashorn", "jdk.nashorn", "org.lwjgl.system",
            "javax.script", "graal.graalvm.polyglot",
            "net.neoforged.fml", "net.neoforged.accesstransformer", "net.neoforged.coremod",
            "cpw.mods.modlauncher", "cpw.mods.gross"
    );

    private ClassFilter() {}

    @Override
    public boolean test(String className) {
        if (!allowThreads && matchesGroup(className, THREAD_GROUP)) return false;
        if (!allowReflection && matchesGroup(className, REFLECT_GROUP)) return false;
        if (!allowAsm && matchesGroup(className, ASM_GROUP)) return false;
        if (matchesGroup(className, GENERAL_BLACKLIST)) return false;
        return true;
    }

    private boolean matchesGroup(String className, Set<String> group) {
        return group.stream().anyMatch(className::startsWith);
    }

    public static boolean isAnyUnsafeFeatureEnabled() {
        return allowThreads || allowReflection || allowAsm;
    }
}