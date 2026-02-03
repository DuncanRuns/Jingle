package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.PidUtil;
import xyz.duncanruns.jingle.util.WindowTitleUtil;
import xyz.duncanruns.jingle.win32.User32;

import java.util.*;

/**
 * The instance checker will run checks for Minecraft instances every second while Jingle has missing instances.
 * When it finds new instances, it will send an InstancesFoundQMessage to Jingle.
 * Additionally, it will remember all opened Minecraft instances on the computer for whenever needed (redetect instances).
 */
public final class InstanceChecker {

    private static final Set<OpenedInstanceInfo> OPENED_INSTANCE_INFOS = new HashSet<>();
    private static Map<HWND, Integer> lastCheckedWindows = new HashMap<>();

    private InstanceChecker() {
    }

    /**
     * Checks for new windows, then checks if they are minecraft windows and gets their path.
     * <p>
     * A general expected execution time is as follows:
     * <li>0.8ms-5ms when there are new non-Minecraft windows</li>
     * <li>300ms or more when there are new Minecraft windows</li>
     * <li>Otherwise 0.5ms-1.2ms</li>
     */
    private static void runChecks() {
        Map<HWND, Integer> checkedWindows = new HashMap<>();
        List<Integer> newPids = HermesInstanceDepot.get().getNewPids();
        // Recheck windows that we now have hermes info for
        lastCheckedWindows.entrySet().removeIf(e -> newPids.contains(e.getValue()));

        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            // Add the window to checked windows
            checkedWindows.put(hWnd, -1);
            // Return if the window was in the last checked windows
            if (lastCheckedWindows.containsKey(hWnd)) {
                return true;
            }
            int pid;
            try {
                pid = PidUtil.getPidFromHwnd(hWnd);
            } catch (Exception e) {
                // Not allowed?
                return true;
            }
            checkedWindows.put(hWnd, pid);
            Optional<JsonObject> hermesInfo = HermesInstanceDepot.get().getInstance(pid);
            if (hermesInfo.isPresent()) {
                OPENED_INSTANCE_INFOS.add(OpenedInstanceInfo.getInstanceInfoFromHermes(hermesInfo.get(), hWnd, pid));
                return true;
            }
            // Get the title, return if it is not a minecraft title
            String title = WindowTitleUtil.getHwndTitle(hWnd);
            if (!WindowTitleUtil.matchesMinecraft(title)) {
                return true;
            }
            Jingle.log(Level.DEBUG, "InstanceChecker: Minecraft title matched: " + title);
            // Get instance info, return if failing to get the path
            InstanceInfo instanceInfo = InstanceInfo.getInstanceInfoFromHwnd(hWnd, pid);
            if (instanceInfo == null) {
                Jingle.log(Level.DEBUG, "InstanceChecker: FoundInstanceInfo invalid!");
                return true;
            }
            Jingle.log(Level.DEBUG, "InstanceChecker: FoundInstanceInfo found.");
            // Create the instance object
            // Add the minecraft instance to the set of opened instances
            OPENED_INSTANCE_INFOS.add(new OpenedInstanceInfo(instanceInfo, hWnd, pid));
            Jingle.log(Level.DEBUG, "InstanceChecker: Added instance to opened instances.");

            return true;
        }, null);

        // Remove any opened instance windows that are NOT REAL!!!
        OPENED_INSTANCE_INFOS.removeIf(i -> !User32.INSTANCE.IsWindow(i.hwnd));
        // Replace the last checked windows set
        lastCheckedWindows = checkedWindows;
    }

    /**
     * Warning: can be blocking for hundreds of milliseconds!
     */
    public synchronized static Set<OpenedInstanceInfo> getAllOpenedInstances() {
        runChecks();
        // Return a set with lazy copies of the opened instances
        return new HashSet<>(OPENED_INSTANCE_INFOS);
    }
}
