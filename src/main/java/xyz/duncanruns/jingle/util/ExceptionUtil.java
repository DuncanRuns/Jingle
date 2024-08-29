package xyz.duncanruns.jingle.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

public final class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static String toDetailedString(Throwable t) {
        StringWriter out = new StringWriter();
        out.write(t.toString() + "\n");
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    public static Throwable getRootCause(Throwable t) {
        if (t == null) return null;
        Set<Throwable> seen = new HashSet<>();
        while (t.getCause() != null && !seen.contains(t)) {
            seen.add(t);
            t = t.getCause();
        }
        return t;
    }
}
