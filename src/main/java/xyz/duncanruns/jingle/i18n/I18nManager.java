package xyz.duncanruns.jingle.i18n;

import xyz.duncanruns.jingle.Jingle;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class I18nManager {
    private static ResourceBundle bundle;

    public static void init(@Nullable String langCode) {
        Locale locale;

        if (langCode != null && !langCode.isEmpty()) {
            String[] parts = langCode.split("_");
            locale = parts.length > 1 ?
                    new Locale(parts[0], parts[1]) :
                    new Locale(parts[0]);
        } else {
            locale = Locale.getDefault();
        }

        try {
            bundle = ResourceBundle.getBundle("i18n.Jingle", locale, new UTF8Control());
        } catch (Exception e) {
            Jingle.logError("Failed to load resource bundle for locale: " + locale, e);
            try {
                bundle = ResourceBundle.getBundle("i18n.Jingle", Locale.US, new UTF8Control());
            } catch (Exception e2) {
                Jingle.logError("Failed to load default resource bundle", e2);
                bundle = new EmptyResourceBundle();
            }
        }
    }

    // custom ResourceBundle.Control support UTF-8
    private static class UTF8Control extends ResourceBundle.Control {
        public ResourceBundle newBundle(
                String baseName,
                Locale locale,
                String format,
                ClassLoader loader,
                boolean reload
        ) throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream = null;

            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }

            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
            return null;
        }
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    private static class EmptyResourceBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    }

}
