package xyz.duncanruns.jingle.util;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {
    private static final Pattern FIRST_NUM_PATTERN = Pattern.compile("^.*?(\\d+).*?$");

    /**
     * Guesses the major java version by looking at the java.version system property.
     */
    public static Optional<Integer> guessMajorJavaVersion() {
        String s = System.getProperty("java.version");
        if (s.startsWith("1.")) s = s.substring(2);
        Matcher matcher = FIRST_NUM_PATTERN.matcher(s);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    /**
     * Gets the major java version by using the Runtime.version() method.
     */
    public static Optional<Integer> getMajorJavaVersion() {
        try {
            Method versionMethod = Runtime.class.getMethod("version");
            Object version = versionMethod.invoke(Runtime.getRuntime());
            Method majorMethod = version.getClass().getMethod("major");
            return Optional.of((Integer) majorMethod.invoke(version));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to obtain Runtime version", e);
        }
    }
}
