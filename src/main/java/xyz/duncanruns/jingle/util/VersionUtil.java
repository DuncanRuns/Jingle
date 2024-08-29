package xyz.duncanruns.jingle.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtil {
    private static final Pattern GENERAL_VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)*");

    private VersionUtil() {
    }

    public static int tryCompare(String versionA, String versionB, int onFailure) {
        try {
            return Version.of(versionA).compareTo(Version.of(versionB));
        } catch (Exception e) {
            return onFailure;
        }
    }

    public static int compare(String versionA, String versionB) {
        return Version.of(versionA).compareTo(Version.of(versionB));
    }

    /**
     * Returns the first version-like substring of the input string. Version-like can be any number on its own, or with any amount of dot-separated numbers.
     * Returns the string "0" if the input string is null or has no version-like substring.
     */
    public static String extractVersion(String string) {
        if (string == null) {
            return null;
        }
        Matcher matcher = GENERAL_VERSION_PATTERN.matcher(string);
        if (matcher.find()) {
            return matcher.group();
        }
        return "0";
    }

    /*
     * Version class from https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
     */
    public static class Version implements Comparable<Version> {

        private final String version;

        private Version(String version) {
            if (version == null) {
                throw new IllegalArgumentException("Version can not be null");
            }
            if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
                throw new IllegalArgumentException("Invalid version format");
            }
            this.version = version;
        }

        public static Version of(String versionString) {
            return new Version(versionString);
        }

        public final String get() {
            return this.version;
        }

        @Override
        public int compareTo(Version that) {
            if (that == null) {
                return 1;
            }
            String[] thisParts = this.get().split("\\.");
            String[] thatParts = that.get().split("\\.");
            int length = Math.max(thisParts.length, thatParts.length);
            for (int i = 0; i < length; i++) {
                int thisPart = i < thisParts.length ?
                        Integer.parseInt(thisParts[i]) : 0;
                int thatPart = i < thatParts.length ?
                        Integer.parseInt(thatParts[i]) : 0;
                if (thisPart < thatPart) {
                    return -1;
                }
                if (thisPart > thatPart) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null) {
                return false;
            }
            if (this.getClass() != that.getClass()) {
                return false;
            }
            return this.compareTo((Version) that) == 0;
        }

        @Override
        public String toString() {
            return this.version;
        }
    }
}
