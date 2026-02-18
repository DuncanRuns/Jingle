package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.PsapiUtil;
import com.sun.jna.platform.win32.WinDef;
import me.duncanruns.kerykeion.listeners.HermesInstanceListener;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.PidUtil;
import xyz.duncanruns.jingle.util.WindowTitleUtil;
import xyz.duncanruns.jingle.win32.User32;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The instance checker will run checks for Minecraft instances every second while Jingle has missing instances.
 * When it finds new instances, it will send an InstancesFoundQMessage to Jingle.
 * Additionally, it will remember all opened Minecraft instances on the computer for whenever needed (redetect instances).
 */
public final class InstanceChecker {

    private static final Set<OpenedInstance> INSTANCES = new HashSet<>();

    private static final Map<Integer, WinDef.HWND> PID_TO_MC_WINDOW = new HashMap<>();
    private static final Set<Integer> PIDS_WITH_WINDOW = new HashSet<>();
    private static final Set<Integer> QUERIED_PIDS = new HashSet<>();
    private static Set<WinDef.HWND> lastCheckedHwnds = new HashSet<>();

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
        Set<WinDef.HWND> existingWindows = new HashSet<>();
        AtomicInteger expensiveCounter = new AtomicInteger();
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            existingWindows.add(hWnd);
            if (lastCheckedHwnds.contains(hWnd)) return true;
            expensiveCounter.incrementAndGet();
            String title = WindowTitleUtil.getHwndTitle(hWnd);
            int pid = PidUtil.getPidFromHwnd(hWnd);
            PIDS_WITH_WINDOW.add(pid);
            if (WindowTitleUtil.matchesMinecraft(title)) {
                Jingle.log(Level.DEBUG, "Found Minecraft window: " + hWnd + " with pid " + pid);
                PID_TO_MC_WINDOW.put(pid, hWnd);
            }
            return true;
        }, null);
        if (expensiveCounter.get() > 0)
            Jingle.log(Level.DEBUG, "Ran " + expensiveCounter + " expensive checks during EnumWindows.");
        lastCheckedHwnds = existingWindows;

        Set<Integer> existingPids = new HashSet<>();
        expensiveCounter.set(0);
        for (int pid : PsapiUtil.enumProcesses()) {
            existingPids.add(pid);
            if (INSTANCES.stream().anyMatch(i -> i.pid == pid)) continue;

            WinDef.HWND minecraftHwnd = PID_TO_MC_WINDOW.getOrDefault(pid, null);

            // Never check a process that isn't associated with a window
            if (!PIDS_WITH_WINDOW.contains(pid)) continue;

            if (minecraftHwnd == null) continue;

            if (QUERIED_PIDS.contains(pid)) continue;

            expensiveCounter.incrementAndGet();
            InstanceInfo instanceInfoFromHwnd = InstanceInfo.getInstanceInfoFromHwnd(pid);
            if (instanceInfoFromHwnd == null) {
                QUERIED_PIDS.add(pid);
                Jingle.log(Level.WARN, "Found a process with a matching Minecraft window, but couldn't obtain instance info.");
                continue;
            }
            OpenedInstance instance = OpenedInstance.getInstanceFromOther(instanceInfoFromHwnd, pid);
            instance.checkWindow(minecraftHwnd);
            INSTANCES.add(instance);
        }
        if (expensiveCounter.get() > 0)
            Jingle.log(Level.DEBUG, "Ran " + expensiveCounter + " expensive checks during enumProcesses.");

        INSTANCES.removeIf(i -> !existingPids.contains(i.pid));
        PIDS_WITH_WINDOW.removeIf(pid -> !existingPids.contains(pid));
        QUERIED_PIDS.removeIf(pid -> !existingPids.contains(pid));
        PID_TO_MC_WINDOW.entrySet().removeIf(e -> (!existingPids.contains(e.getKey())) || !existingWindows.contains(e.getValue()));
    }

    /**
     * Warning: can be blocking for hundreds of milliseconds!
     */
    public synchronized static Set<OpenedInstance> getAllOpenedInstances() {
        runChecks();
        // Return a set with lazy copies of the opened instances
        return new HashSet<>(INSTANCES);
    }

    /**
     * Collects instance infos from Hermes/Kerykeion.
     * Should be not used by plugins.
     */
    public static class HermesChecker implements HermesInstanceListener {
        private static final HermesChecker INSTANCE = new HermesChecker();

        public static HermesChecker get() {
            return INSTANCE;
        }

        @Override
        public void onNewInstance(JsonObject jsonObject, boolean b) {
            try {
                if (jsonObject.get("is_server").getAsBoolean()) return;
                int pid = jsonObject.get("pid").getAsInt();

                Optional<OpenedInstance> existingInstance = INSTANCES.stream().filter(i -> i.pid == pid).findAny();
                if (existingInstance.isPresent()) {
                    // Update instance
                    OpenedInstance instanceToReplace = existingInstance.get();
                    Jingle.log(Level.DEBUG, "Updating instance with Hermes info.");
                    instanceToReplace.updateWithHermes(jsonObject);
                    return;
                }
                Jingle.log(Level.DEBUG, "Creating new instance from Hermes.");
                OpenedInstance newInstance = OpenedInstance.getInstanceFromHermes(jsonObject, pid);
                INSTANCES.add(newInstance);
            } catch (Exception e) {
                Jingle.log(Level.ERROR, "Failed to get pid from Hermes instance: " + ExceptionUtil.toDetailedString(e));
            }
        }

        @Override
        public void onInstanceClosed(JsonObject jsonObject) {
        }
    }
}
