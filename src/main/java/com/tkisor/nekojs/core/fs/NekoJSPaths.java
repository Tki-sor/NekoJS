package com.tkisor.nekojs.core.fs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tkisor.nekojs.NekoJS;
import com.tkisor.nekojs.bindings.event.ModifyWorkspaceConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class NekoJSPaths {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean disableStrictSandbox = false;

    /* ================= Base ================= */
    public static final Path GAME_DIR = FMLPaths.GAMEDIR.get().normalize().toAbsolutePath();
    public static final Path ROOT = GAME_DIR.resolve("nekojs");

    /* ================= Script Folders ================= */
    public static final Path COMMON_SCRIPTS = ROOT.resolve("common_scripts");
    public static final Path STARTUP_SCRIPTS = ROOT.resolve("startup_scripts");
    public static final Path SERVER_SCRIPTS = ROOT.resolve("server_scripts");
    public static final Path CLIENT_SCRIPTS = ROOT.resolve("client_scripts");

    public static final Path PROBE_DIR = GAME_DIR.resolve(".probe");

    public static final Path NODE_MODULES = ROOT.resolve("node_modules");

    /* ================= Config & DX ================= */
    public static final Path CONFIG = ROOT.resolve("config");
    public static final Path README = ROOT.resolve("README.txt");
    public static final Path ENGINE_CONFIG = CONFIG.resolve("engine.properties");

    /* ================= Initialization ================= */
    public static void initFoldersOnly() {
        ensureDir(ROOT);
        ensureDir(COMMON_SCRIPTS);
        ensureDir(STARTUP_SCRIPTS);
        ensureDir(SERVER_SCRIPTS);
        ensureDir(CLIENT_SCRIPTS);
        ensureDir(CONFIG);
        ensureDir(PROBE_DIR);
        ensureDir(NODE_MODULES);

        createReadme();
        loadEngineConfig();
    }

    /* ================= Utilities ================= */
    private static void ensureDir(Path dir) {
        try {
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            NekoJS.LOGGER.error("[NekoJS] 无法创建目录: {}", dir, e);
        }
    }

    private static void loadEngineConfig() {
        Properties props = new Properties();
        try {
            if (Files.exists(ENGINE_CONFIG)) {
                try (InputStream is = Files.newInputStream(ENGINE_CONFIG)) {
                    props.load(is);
                    disableStrictSandbox = Boolean.parseBoolean(props.getProperty("disableStrictSandbox", "false"));
                }
            } else {
                props.setProperty("disableStrictSandbox", "false");
                try (OutputStream os = Files.newOutputStream(ENGINE_CONFIG)) {
                    props.store(os, "NekoJS Engine Core Configuration\n" +
                            "# WARNING: Setting disableStrictSandbox to true will allow scripts to access dangerous Java classes (e.g., IO, Reflection).");
                }
            }
            NekoJS.LOGGER.info("[NekoJS] 引擎配置加载完毕。沙盒禁用状态: {}", disableStrictSandbox);
        } catch (Exception e) {
            NekoJS.LOGGER.error("[NekoJS] 无法加载 engine.properties", e);
        }
    }

    public static Path verifyInsideGameDir(Path path) throws IOException {
        Path normalized = path.normalize().toAbsolutePath();
        if (!normalized.startsWith(GAME_DIR)) {
            throw new IOException("Access outside game directory is forbidden");
        }
        if (Files.exists(normalized)) {
            Path realPath = normalized.toRealPath();
            if (!realPath.startsWith(GAME_DIR)) {
                throw new IOException("Symlink escape detected!");
            }
        }
        return normalized;
    }

    private static void createReadme() {
        if (Files.notExists(README)) {
            try {
                Files.writeString(README, """
                      === NekoJS 脚本目录说明 ===
                      - startup_scripts: 游戏启动时加载，用于注册物品、方块。修改后需要重启游戏。
                      - server_scripts: 存档/服务器加载时运行，用于配方、事件监听。可使用 /reload 重载。
                      - client_scripts: 仅在客户端运行，用于 GUI、按键绑定等。
                      - 提示: 自动生成的类型声明文件 (.d.ts) 位于游戏根目录的 .probe 文件夹中，请勿手动修改。
                      """.trim());
            } catch (Exception ex) {
                NekoJS.LOGGER.error("[NekoJS] 无法创建 README.txt 文件", ex);
            }
        }
    }

    public static void createWorkspaceConfigs() {
        createConfigForEnv("server", SERVER_SCRIPTS);
        createConfigForEnv("client", CLIENT_SCRIPTS);
        createConfigForEnv("startup", STARTUP_SCRIPTS);
        createConfigForEnv("common", COMMON_SCRIPTS);
    }

    private static void createConfigForEnv(String envName, Path scriptDir) {
        JSConfigModel model = new JSConfigModel();

        String relativeProbePath = "../../.probe/" + envName + "/probe-types";

        model.compilerOptions.typeRoots = List.of(
                relativeProbePath,
                "../node_modules/@types"
        );

        model.compilerOptions.moduleResolution = "node";

        model.compilerOptions.baseUrl = relativeProbePath;

        ModifyWorkspaceConfigEvent event = new ModifyWorkspaceConfigEvent(model, envName);
        NeoForge.EVENT_BUS.post(event);

        Path configPath = scriptDir.resolve(event.getFileName());

        if (Files.notExists(configPath)) {
            try {
                String jsonContent = GSON.toJson(event.getModel());
                Files.writeString(configPath, jsonContent);
            } catch (Exception e) {
                NekoJS.LOGGER.error("[NekoJS] 无法创建配置文件: {}", configPath, e);
            }
        }
    }

    private NekoJSPaths() {}
}