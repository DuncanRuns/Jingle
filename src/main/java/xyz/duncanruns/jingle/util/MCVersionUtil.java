package xyz.duncanruns.jingle.util;

import xyz.duncanruns.jingle.util.VersionUtil.Version;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MCVersionUtil {
    // SNAP_MAP is a map to help convert snapshots to a comparable version string
    // The integer arrays will always contain 3 integers, the first being the year the snapshot should be from, then the minimum and maximum week number
    // Snapshots for major version updates should be converted to a .99 of the former major version
    // Snapshots for minor version updates should be rounded to the latter minor version
    private static final Map<int[], String> SNAP_MAP = getSnapMap();
    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("^(\\d\\d)w0?(\\d+).+"); // Does not exactly match, but is useful for matching groups

    private static final String DEFAULT_OLD_SNAPSHOT_VERSION = "0.99.99";
    private static final String DEFAULT_NEW_SNAPSHOT_VERSION = "1.21.7";

    private MCVersionUtil() {
    }

    private static Map<int[], String> getSnapMap() {
        Map<int[], String> map = new HashMap<>();

        map.put(new int[]{11, 47, 52}, "1.0.99"); // 2011 1.1 snapshots
        map.put(new int[]{12, 1, 1}, "1.1.1"); // 2012 1.1 snapshots
        map.put(new int[]{12, 3, 8}, "1.1.99"); // 1.2 snapshots
        map.put(new int[]{12, 15, 30}, "1.2.99"); // 1.3 snapshots
        map.put(new int[]{12, 32, 42}, "1.3.99"); // 1.4 snapshots
        map.put(new int[]{13, 1, 10}, "1.4.99"); // 1.5 snapshots
        map.put(new int[]{13, 11, 12}, "1.5.1"); // 1.5.1 snapshots
        map.put(new int[]{13, 16, 26}, "1.5.99"); // 1.6 snapshots
        map.put(new int[]{13, 36, 43}, "1.6.99"); // 1.7 snapshots
        map.put(new int[]{13, 47, 49}, "1.6.99"); // 1.7.4 snapshots
        map.put(new int[]{14, 2, 34}, "1.7.99"); // 1.8 snapshots
        map.put(new int[]{15, 31, 52}, "1.8.99"); // 2015 1.9 snapshots
        map.put(new int[]{16, 1, 7}, "1.8.99"); // 2016 1.9 snapshots
        map.put(new int[]{16, 14, 15}, "1.9.3"); // 1.9.3 snapshots
        map.put(new int[]{16, 20, 21}, "1.9.99"); // 1.10 snapshots
        map.put(new int[]{16, 32, 44}, "1.10.99"); // 1.11 snapshots
        map.put(new int[]{16, 50, 50}, "1.11.1"); // 1.11.1 snapshots
        map.put(new int[]{17, 6, 18}, "1.11.99"); // 1.12 snapshots
        map.put(new int[]{17, 31, 31}, "1.12.1"); // 1.12.1 snapshots
        map.put(new int[]{17, 43, 52}, "1.12.99"); // 2017 1.13 snapshots
        map.put(new int[]{18, 1, 22}, "1.12.99"); // 2018 1.13 snapshots
        map.put(new int[]{18, 30, 33}, "1.13.1"); // 1.13.1 snapshots
        map.put(new int[]{18, 43, 52}, "1.13.99"); // 2018 1.14 snapshots
        map.put(new int[]{19, 1, 14}, "1.14.99"); // 2019 1.14 snapshots
        map.put(new int[]{19, 34, 46}, "1.14.99"); // 1.15 snapshots
        map.put(new int[]{20, 6, 22}, "1.15.99"); // 1.16 snapshots, including infinity april fools
        map.put(new int[]{20, 27, 30}, "1.16.2"); // 1.16.2 snapshots
        map.put(new int[]{20, 45, 51}, "1.16.99"); // 1.17 snapshots
        map.put(new int[]{21, 3, 20}, "1.16.99"); // 1.17 snapshots
        map.put(new int[]{21, 37, 44}, "1.17.99"); // 1.18 snapshots
        map.put(new int[]{22, 3, 7}, "1.18.2"); // 1.18.2 snapshots
        map.put(new int[]{22, 11, 19}, "1.18.99"); // 1.19 snapshots, including one block at a time april fools
        map.put(new int[]{22, 24, 24}, "1.19.1"); // 1.19.1 snapshot
        map.put(new int[]{22, 42, 46}, "1.19.3"); // 1.19.3 snapshots
        map.put(new int[]{23, 3, 7}, "1.19.4"); // 1.19.4 snapshots
        map.put(new int[]{23, 12, 52}, "1.19.99"); // 1.20 snapshots, including voting april fools
        map.put(new int[]{23, 51, 51}, "1.20.5"); // 2023 1.20.5 snapshots
        map.put(new int[]{24, 3, 14}, "1.20.5"); // 2024 1.20.5 snapshots, including potato april fools
        map.put(new int[]{24, 18, 21}, "1.20.99"); // 1.21 snapshots
        map.put(new int[]{24, 33, 39}, "1.21.2"); // 1.21.2 snapshots
        map.put(new int[]{24, 44, 46}, "1.21.4"); // 1.21.4 snapshots
        map.put(new int[]{25, 2, 10}, "1.21.5"); // 1.21.5 snapshots
        map.put(new int[]{25, 15, 21}, "1.21.6"); // 1.21.6 snapshots

        return Collections.unmodifiableMap(map);
    }

    public static Version findVersion(String versionString) {
        try {
            // hack for pre-releases & release candidates
            int idx = versionString.indexOf('-');
            if (idx != -1) {
                versionString = versionString.substring(0, idx);
            }
            return Version.of(versionString);
        } catch (IllegalArgumentException e) {
            // Check for snapshots
            return getSnapshotVersion(versionString);
        }
    }

    private static Version getSnapshotVersion(String snapshotVersion) {
        Matcher m = SNAPSHOT_VERSION_PATTERN.matcher(snapshotVersion);
        if (m.matches()) {
            int year = Integer.parseInt(m.group(1));
            int week = Integer.parseInt(m.group(2));
            for (Map.Entry<int[], String> entry : SNAP_MAP.entrySet()) {
                int[] searchInts = entry.getKey();
                if (year == searchInts[0] && week >= searchInts[1] && week <= searchInts[2]) {
                    return Version.of(entry.getValue());
                }
            }

            if (year < 12) return Version.of(DEFAULT_OLD_SNAPSHOT_VERSION);
            return Version.of(DEFAULT_NEW_SNAPSHOT_VERSION);
        }
        return null;
    }

    /**
     * versionY should not refer to a snapshot, to check for equaling an exact snapshot, use {@link String#equals(Object) String.equals()}.
     *
     * @return true if versionX is older than versionY, otherwise false
     */
    public static boolean isOlderThan(String versionX, String versionY) {
        return findVersion(versionX).compareTo(findVersion(versionY)) < 0;
    }

    /**
     * versionY should not refer to a snapshot, to check for equaling an exact snapshot, use {@link String#equals(Object) String.equals()}.
     *
     * @return true if versionX is newer than versionY, otherwise false
     */
    public static boolean isNewerThan(String versionX, String versionY) {
        return findVersion(versionX).compareTo(findVersion(versionY)) > 0;
    }

    /**
     * <li>2 stable releases are not loosely equals (ie. 1.14.3 != 1.14.4)</li>
     * <li>2 snapshots for the same version are loosely equal (ie. 22w43a = 22w46a)</li>
     * <li>A snapshot for a minor release equals the minor release (ie. 22w06a = 1.18.2</li>
     * <li>A snapshot for a major release never equals any stable release (ie. 21w42a != 1.18)</li>
     * <p>
     * Equal checking should just use {@link String#equals(Object) String.equals()}, this method does not work for checking exact snapshots.
     *
     * @return true if versionX is the same version as versionY, otherwise false
     */
    public static boolean isLooselyEqual(String versionX, String versionY) {
        return findVersion(versionX).compareTo(findVersion(versionY)) == 0;
    }
}
