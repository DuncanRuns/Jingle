package xyz.duncanruns.jingle.util;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class LegalModsUtil {
    private static Set<String> legalMods = Arrays.stream(
            // Start with default legal mods in case updating fails
            "planifolia, phosphor, antiresourcereload, forceport, chunkumulator, fabricproxylite, lithium, voyager, dynamicmenufps, nopaus, sleepbackground, setspawnmod, sodiummac, fastreset, legacycrashfix, statsperworld, chunkcacher, speedrunigt, atum, speedrunapi, retino, standardsettings, antigone, lazydfu, lazystronghold, optifabric, starlight, extraoptions, zbufferfog, sodium, stateoutput, tabfocus, krypton, worldpreview, anchiale, biomethreadlocalfix, seedqueue, costar"
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
        return GrabUtil.grabJson("https://raw.githubusercontent.com/tildejustin/mcsr-meta/schema-6/mods.json")
                .getAsJsonArray("mods").asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(j -> j.get("modid").getAsString().replaceAll("[-_]", "")).collect(Collectors.toSet());
    }
}