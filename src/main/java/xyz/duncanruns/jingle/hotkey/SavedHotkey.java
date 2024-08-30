package xyz.duncanruns.jingle.hotkey;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SavedHotkey {

    public final String type;
    public final String action;
    public final List<Integer> keys;
    public final boolean ignoreModifiers;

    public SavedHotkey(String type, String action, List<Integer> keys, boolean ignoreModifiers) {
        this.type = type;
        this.action = action;
        this.keys = keys;
        this.ignoreModifiers = ignoreModifiers;
    }

    public static Optional<SavedHotkey> fromJson(JsonObject hotkeyJson) {
        if (!(hotkeyJson.has("type") && hotkeyJson.has("action") && hotkeyJson.has("keys") && hotkeyJson.get("keys").isJsonArray()))
            return Optional.empty();
        String action = hotkeyJson.get("action").getAsString();
        String type = hotkeyJson.get("type").getAsString();
        List<Integer> keys = Hotkey.keysFromJson(hotkeyJson.getAsJsonArray("keys"));

        boolean ignoreModifiers = hotkeyJson.has("ignoreModifiers") && hotkeyJson.get("ignoreModifiers").getAsBoolean();
        return Optional.of(new SavedHotkey(type, action, keys, ignoreModifiers));
    }

    public JsonObject toJson() {
        JsonObject hotkeyJson = new JsonObject();
        hotkeyJson.addProperty("type", this.type);
        hotkeyJson.addProperty("action", this.action);
        hotkeyJson.addProperty("ignoreModifiers", this.ignoreModifiers);
        hotkeyJson.add("keys", Hotkey.jsonFromKeys(this.keys));
        return hotkeyJson;
    }

    @Override
    public int hashCode() {
        int result = this.type.hashCode();
        result = 31 * result + this.action.hashCode();
        result = 31 * result + this.keys.hashCode();
        result = 31 * result + Boolean.hashCode(this.ignoreModifiers);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        SavedHotkey that = (SavedHotkey) o;
        return this.ignoreModifiers == that.ignoreModifiers && Objects.equals(this.type, that.type) && Objects.equals(this.action, that.action) && Objects.equals(this.keys, that.keys);
    }
}
