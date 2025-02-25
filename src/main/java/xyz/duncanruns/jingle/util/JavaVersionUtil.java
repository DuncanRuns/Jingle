package xyz.duncanruns.jingle.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {
    private static final Pattern FIRST_NUM_PATTERN = Pattern.compile("^.*?(\\d+).*?$");

    public static int getMajorJavaVersion() {
        String s = System.getProperty("java.version");
        if (s.startsWith("1.")) s = s.substring(2);
        Matcher matcher = FIRST_NUM_PATTERN.matcher(s);
        if (!matcher.matches()) return -1;
        return Integer.parseInt(matcher.group(1));
    }
}
