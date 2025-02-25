package xyz.duncanruns.jingle.plugin;

import com.google.gson.Gson;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.JavaVersionUtil;
import xyz.duncanruns.jingle.util.ResourceUtil;
import xyz.duncanruns.jingle.util.VersionUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PluginManager {
    private static final Gson GSON = new Gson();
    private static final Path PLUGINS_PATH = Jingle.FOLDER.resolve("plugins").toAbsolutePath();

    private static final List<LoadedJinglePlugin> loadedPlugins = new ArrayList<>();
    private static final Set<String> pluginCollisions = new HashSet<>();

    private PluginManager() {
    }

    public static Path getPluginsPath() {
        return PLUGINS_PATH;
    }

    private static Runnable importJar(File file, String initializer) throws Exception {
        // https://stackoverflow.com/questions/11016092/how-to-load-classes-at-runtime-from-a-folder-or-jar
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> e = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + file + "!/")};
        URLClassLoader cl = URLClassLoader.newInstance(urls);

        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            try {
                cl.loadClass(className);
            } catch (Error nce) {
                // A fabric fail class is a class meant to crash loading with fabric. Useful to make sure players don't try to use Jingle plugins as a fabric mod.
                // Jingle fails to load them since they refer to a class that doesn't exist, so we ignore it.
                boolean isFabricFailClass = nce.getMessage().contains("net/fabricmc/api/ModInitializer");
                if (!isFabricFailClass) {
                    // If it is not a fabric fail class, we do want to warn for this
                    Jingle.logError("Failed to load class '" + className + "'! Jingle may crash if this is needed by a plugin...:", nce);
                }
            }
        }

        jarFile.close();

        String[] initializerParts = initializer.split("::");
        if (initializerParts.length >= 3) {
            throw new PluginInitializationException("Invalid path to plugin initializer!");
        }
        String className = initializerParts[0];
        String methodName = initializerParts.length == 2 ? initializerParts[1] : "init";
        if (methodName.isEmpty()) {
            throw new PluginInitializationException("Invalid path to plugin initializer!");
        }
        Class<?> clazz = cl.loadClass(className);
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            return () -> {
                try {
                    method.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            };
        }

        throw new PluginInitializationException("Plugin Initializer not found!");
    }

    @SuppressWarnings("all") //Suppress the redundant cast warning which resolves an ambiguous case
    private static String getJarJPJContents(Path jarPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jsonFilePath = fs.getPath("jingle.plugin.json");
            byte[] jsonData = Files.readAllBytes(jsonFilePath);
            return new String(jsonData, StandardCharsets.UTF_8);
        }
    }

    private static void warnWontLoad(JinglePluginData oldData) {
        Jingle.log(Level.WARN, String.format("%s v%s will not load because it is not the newest version detected.", oldData.name, oldData.version));
    }

    public static Set<String> getPluginCollisions() {
        return Collections.unmodifiableSet(pluginCollisions);
    }

    public static List<LoadedJinglePlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(loadedPlugins);
    }

    public static List<Pair<Path, JinglePluginData>> getFolderPlugins() throws IOException {
        if (!Files.exists(PLUGINS_PATH)) {
            try {
                Files.createDirectory(PLUGINS_PATH);
            } catch (Exception ignored) {
            }
            return Collections.emptyList();
        }
        List<Pair<Path, JinglePluginData>> plugins = new ArrayList<>();
        try (Stream<Path> list = Files.list(PLUGINS_PATH)) {
            list.filter(path -> path.getFileName().toString().endsWith(".jar")).forEach(path -> {
                try {
                    JinglePluginData data = JinglePluginData.fromString(getJarJPJContents(path));
                    plugins.add(Pair.of(path, data));
                } catch (Throwable e) {
                    Jingle.log(Level.WARN, "Failed to read plugin " + path + "!\n" + ExceptionUtil.toDetailedString(e));
                }
            });
        }
        return plugins;
    }

    private static List<Pair<Path, JinglePluginData>> getDefaultPlugins() throws IOException, URISyntaxException {
        List<String> fileNames = ResourceUtil.getResourcesFromFolder("defaultplugins").stream().map(s -> "/defaultplugins/" + s).collect(Collectors.toList());

        Jingle.log(Level.DEBUG, "Default Plugins:" + fileNames);

        List<Pair<Path, JinglePluginData>> plugins = new ArrayList<>();

        for (String fileName : fileNames) {
            Path path = Paths.get(File.createTempFile(fileName, null).getPath());
            if (!fileName.startsWith("/")) {
                fileName = "/" + fileName;
            }
            ResourceUtil.copyResourceToFile(fileName, path);
            try {
                JinglePluginData data = JinglePluginData.fromString(getJarJPJContents(path));
                plugins.add(Pair.of(path, data));
            } catch (Exception e) {
                Jingle.log(Level.ERROR, "Failed to read default plugin: " + fileName);
            }
        }
        return plugins;
    }

    public static void loadPlugins() {
        List<Pair<Path, JinglePluginData>> folderPlugins = Collections.emptyList();
        List<Pair<Path, JinglePluginData>> defaultPlugins = Collections.emptyList();
        try {
            folderPlugins = getFolderPlugins();
        } catch (IOException e) {
            Jingle.log(Level.ERROR, "Failed to load plugins from folder: " + ExceptionUtil.toDetailedString(e));
        }
        try {
            defaultPlugins = getDefaultPlugins();
        } catch (IOException | URISyntaxException e) {
            Jingle.log(Level.ERROR, "Failed to load default plugins: " + ExceptionUtil.toDetailedString(e));
        }

        // Mod ID -> path and data
        Map<String, Pair<Path, JinglePluginData>> bestPluginVersions = new HashMap<>();

        Stream.concat(folderPlugins.stream(), defaultPlugins.stream()).forEach(pair -> {
            JinglePluginData data = pair.getRight();

            if (bestPluginVersions.containsKey(data.id)) {
                if (VersionUtil.tryCompare(data.version.split("\\+")[0], bestPluginVersions.get(data.id).getRight().version.split("\\+")[0], 0) > 0) {
                    JinglePluginData oldData = bestPluginVersions.get(data.id).getRight();
                    bestPluginVersions.put(data.id, pair);
                    warnWontLoad(oldData);
                } else {
                    warnWontLoad(data);
                }
            } else {
                bestPluginVersions.put(data.id, pair);
            }
        });

        // Check already loaded plugins (which can only be dev plugins because default and folder aren't registered yet)
        for (LoadedJinglePlugin loadedPlugin : getLoadedPlugins()) {
            String loadedDevPluginID = loadedPlugin.pluginData.id;
            if (bestPluginVersions.containsKey(loadedDevPluginID)) {
                Pair<Path, JinglePluginData> removed = bestPluginVersions.remove(loadedDevPluginID);
                JinglePluginData data = removed.getRight();
                Jingle.log(Level.WARN, String.format("%s v%s will not load because a dev plugin will be initialized instead.", data.name, data.version));
            }
        }

        int majorJavaVersion = JavaVersionUtil.getMajorJavaVersion();
        if (majorJavaVersion == -1) {
            Jingle.log(Level.WARN, "Couldn't find java version! Assuming plugins are all compatible with the used Java...");
            majorJavaVersion = Integer.MAX_VALUE;
        }

        for (Map.Entry<String, Pair<Path, JinglePluginData>> entry : bestPluginVersions.entrySet()) {
            JinglePluginData data = entry.getValue().getRight();
            if (data.minimumJava > majorJavaVersion) {
                Jingle.log(Level.WARN, String.format("%s v%s will not load because Jingle is not running with the minimum required Java version (Java %d).", data.name, data.version, data.minimumJava));
                continue;
            }
            try {
                loadPluginJar(entry.getValue().getLeft(), data);
            } catch (Exception e) {
                Jingle.log(Level.ERROR, "Failed to load plugin from " + entry.getValue().getLeft() + ": " + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private static void loadPluginJar(Path path, JinglePluginData jinglePluginData) throws Exception {
        if (canRegister(jinglePluginData)) {
            Runnable pluginInitializer = importJar(path.toFile(), jinglePluginData.initializer);
            registerPlugin(jinglePluginData, pluginInitializer);
        } else {
            Jingle.log(Level.WARN, "Failed to load plugin " + path + ", because there is another plugin with the same id already loaded.");
            pluginCollisions.add(jinglePluginData.id);
        }
    }

    /**
     * Loads a plugin from a plugin data object and an initializer.
     *
     * @param data the plugin data object
     */
    public static void registerPlugin(JinglePluginData data, Runnable initializer) {
        loadedPlugins.add(new LoadedJinglePlugin(data, initializer));
    }

    private static boolean canRegister(JinglePluginData data) {
        return loadedPlugins.isEmpty() || loadedPlugins.stream().map(p -> p.pluginData).noneMatch(data::matchesOther);
    }

    public static void initializePlugins() {
        loadedPlugins.forEach(plugin -> plugin.pluginInitializer.run());
    }

    public static class JinglePluginData {
        public String name = null;
        public String id = null;
        public String version = null;
        public String initializer = null;
        public int minimumJava = 8;

        public static JinglePluginData fromString(String string) {
            return GSON.fromJson(string, JinglePluginData.class);
        }

        public boolean matchesOther(JinglePluginData other) {
            return other != null && this.id.equals(other.id);
        }
    }

    public static class LoadedJinglePlugin {
        public final JinglePluginData pluginData;
        public final Runnable pluginInitializer;

        private LoadedJinglePlugin(JinglePluginData data, Runnable initializer) {
            this.pluginData = data;
            this.pluginInitializer = initializer;
        }
    }

    public static class PluginInitializationException extends Exception {
        public PluginInitializationException(String message) {
            super(message);
        }
    }
}
