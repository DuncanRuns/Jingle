package xyz.duncanruns.jingle.util;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class LegalModsUtil {
    private static Set<String> legalMods = Arrays.stream(
            // Start with default legal mods in case updating fails
            "anchiale, antigone, antiresourcereload, atum, biomethreadlocalfix, boundlesswindow, chunkcacher, chunkumulator, costar, dynamicmenufps, extraoptions, fabricproxylite, fastreset, forceport, hermes, hermescore, krypton, lazydfu, lazystronghold, legacycrashfix, legacyplanifolia, lithium, mcsrfairplay, nopaus, optifabric, optifabricorigins, optifine, optifinelight, phosphor, planifolia, retino, seedqueue, setspawnmod, sleepbackground, sodium, sodiummac, speedrunapi, speedrunigt, standardsettings, starlight, stateoutput, statsperworld, tabfocus, voyager, worldpreview, zbufferfog"
                    .split(", ")
    ).collect(Collectors.toSet());
    private static boolean updated = false;

    public static void updateLegalMods() throws IOException {
        try {
            legalMods = obtainLegalMods();
        } finally {
            updated = true;
        }
    }

    public static boolean hasUpdated() {
        return updated;
    }

    public static boolean isLegalMod(String modid) {
        return legalMods.contains(modid.toLowerCase().replaceAll("[-_]", ""));
    }

    private static Set<String> obtainLegalMods() throws IOException {
        return GrabUtil.grabJson("https://raw.githubusercontent.com/tildejustin/mcsr-meta/schema-7/mods.json")
                .getAsJsonArray("mods").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(j -> j.get("modid").getAsString().replaceAll("[-_]", "")).collect(Collectors.toSet());
    }

    // Generates the defaults
    public static void main(String[] args) throws IOException {
        System.out.println(obtainLegalMods().stream().sorted().collect(Collectors.joining(", ")));
    }
}