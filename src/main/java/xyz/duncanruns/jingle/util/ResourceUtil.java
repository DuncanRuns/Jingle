package xyz.duncanruns.jingle.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ResourceUtil {
    private ResourceUtil() {

    }

    public static BufferedImage getImageResource(String name) throws IOException {
        return ImageIO.read(getResource(name));
    }

    public static URL getResource(String name) {
        return ResourceUtil.class.getResource(name);
    }

    public static void copyResourceToFile(String resourceName, Path destination) throws IOException {
        // Answer to https://stackoverflow.com/questions/10308221/how-to-copy-file-inside-jar-to-outside-the-jar
        InputStream inStream = getResourceAsStream(resourceName);
        OutputStream outStream = Files.newOutputStream(destination);
        int readBytes;
        byte[] buffer = new byte[4096];
        while ((readBytes = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, readBytes);
        }
        inStream.close();
        outStream.close();
    }

    public static InputStream getResourceAsStream(String name) {
        return ResourceUtil.class.getResourceAsStream(name);
    }

    public static boolean isResourceFolder(String path) {
        if (path.contains(".")) {
            return false;
        }
        try {
            List<String> resources = getResourcesFromFolder(path);
            return resources != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getResourcesFromFolder(String folder) {
        List<String> result;

        try {
            result = getResourcesFromFolderJAR(folder);
        } catch (Exception ignored) {
            result = getResourcesFromFolderDev(folder);
        }

        return result;
    }

    private static List<String> getResourcesFromFolderDev(String folder) {
        if (!folder.startsWith("/")) {
            folder = "/" + folder;
        }
        return Arrays.stream(Objects.requireNonNull(new File(ResourceUtil.class.getResource(folder).getPath()).list())).collect(Collectors.toList());
    }

    private static List<String> getResourcesFromFolderJAR(String folder) throws URISyntaxException, IOException {
        List<String> result;
        // get path of the current running JAR
        String jarPath = ResourceUtil.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath();

        // Encode the jarPath to handle spaces
        jarPath = new File(jarPath).toURI().toString();

        // file list JAR
        URI uri = URI.create("jar:" + jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            result = Files.list(fs.getPath(folder))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        }
        return result;
    }
}
