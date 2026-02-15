package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.PsapiUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The instance checker will run checks for Minecraft instances every second while Jingle has missing instances.
 * When it finds new instances, it will send an InstancesFoundQMessage to Jingle.
 * Additionally, it will remember all opened Minecraft instances on the computer for whenever needed (redetect instances).
 */
public final class InstanceChecker {

    private static final Set<OpenedInstanceInfo> OPENED_INSTANCE_INFOS = new HashSet<>();
    private static Set<Integer> lastCheckedPids = new HashSet<>();

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
        Set<Integer> checkedPids = new HashSet<>();
        List<Integer> newPids = HermesInstanceDepot.get().getNewPids();
        // Recheck windows that we now have hermes info for
        newPids.forEach(lastCheckedPids::remove);

        for (int pid : PsapiUtil.enumProcesses()) {
            checkedPids.add(pid);
            if (lastCheckedPids.contains(pid)) {
                continue;
            }
            HermesInstanceDepot.get()
                    .getInstance(pid)
                    .ifPresent(jsonObject ->
                            OPENED_INSTANCE_INFOS.add(
                                    OpenedInstanceInfo.getInstanceInfoFromHermes(jsonObject, pid)
                            ));
        }

        // Remove any opened instances that are NOT REAL!!!
        OPENED_INSTANCE_INFOS.removeIf(i -> !checkedPids.contains(i.pid));
        // Replace the last checked windows set
        lastCheckedPids = checkedPids;
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
