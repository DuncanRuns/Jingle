package xyz.duncanruns.jingle.hotkey;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginHotkeys;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.KeyboardUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Hotkey {

    private static final Hotkey EMPTY = Hotkey.of(Collections.emptyList());
    protected final List<Integer> keys;
    private boolean hasBeenPressed;

    private Hotkey(List<Integer> keys) {
        // Copy the list by wrapping in the ArrayList constructor, and use unmodifiableList to give an unmodifiable view of it.
        // This is the best way to prevent the hotkey from being tampered with, which also keeps it thread-safe.
        this.keys = new ArrayList<>(keys);
        this.hasBeenPressed = false;
    }

    public static List<Integer> keysFromJson(JsonArray jsonArray) {
        return jsonArray.asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isNumber)
                .map(JsonPrimitive::getAsInt)
                .collect(Collectors.toList());
    }

    public static String formatKeys(List<Integer> keys) {
        return new Hotkey(keys).toString();
    }

    public static Hotkey empty() {
        return EMPTY;
    }

    public static Hotkey of(List<Integer> keys) {
        return new Hotkey(keys);
    }

    public static Hotkey of(List<Integer> keys, boolean ignoreModifiers) {
        return ignoreModifiers ? new HotkeyIM(keys) : new Hotkey(keys);
    }

    /**
     * This method is <b>non-blocking</b>.
     * Starts a background task which waits for a hotkey to be pressed. Once a hotkey is pressed, it will be accepted
     * by the hotkeyConsumer. The background task will consistently check the shouldContinueFunction, it is important
     * that the method does not take long to process.
     *
     * @param shouldContinueFunction a method which is checked in the background task loop to determine if the task should continue
     * @param hotkeyConsumer         a method which should accept a hotkey once found
     */
    public static void onNextHotkey(BooleanSupplier shouldContinueFunction, Consumer<Hotkey> hotkeyConsumer) {
        List<Integer> preHeldKeys = KeyboardUtil.getPressedKeys();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean done = new AtomicBoolean(false);
        List<Integer> lastHeldKeys = new ArrayList<>();
        executor.scheduleWithFixedDelay(() -> {
            if (done.get()) {
                return;
            }
            if (!shouldContinueFunction.getAsBoolean()) {
                executor.shutdown();
                done.set(true);
                return;
            }
            preHeldKeys.removeIf(key -> !KeyboardUtil.getPressedKeys().contains(key));
            List<Integer> pressedKeys = KeyboardUtil.getPressedKeys(preHeldKeys);

            if (pressedKeys.equals(lastHeldKeys)) return;

            // If keys have been let go
            if (pressedKeys.size() < lastHeldKeys.size()) {
                ArrayList<Integer> keysReleased = new ArrayList<>(lastHeldKeys);
                keysReleased.removeAll(pressedKeys);
                // If the keys let go are not modifiers or all the last held keys are modifiers
                if (KeyboardUtil.ALL_MODIFIERS.containsAll(lastHeldKeys) || keysReleased.stream().noneMatch(KeyboardUtil.ALL_MODIFIERS::contains)) {
                    hotkeyConsumer.accept(new Hotkey(lastHeldKeys));
                    executor.shutdown();
                    return;
                }
            }
            lastHeldKeys.clear();
            lastHeldKeys.addAll(pressedKeys);
        }, 25, 25, TimeUnit.MILLISECONDS);

    }

    public static JsonArray jsonFromKeys(List<Integer> keys) {
        JsonArray ja = new JsonArray();
        keys.forEach(ja::add);
        return ja;
    }

    public static List<HotkeyTypeAndAction> getHotkeyActions() {
        return Streams.concat(
                Jingle.getBuiltinHotkeyActionNames().stream().sorted().map(s -> new HotkeyTypeAndAction("builtin", s)),
                PluginHotkeys.getHotkeyActionNames().stream().sorted().map(s -> new HotkeyTypeAndAction("plugin", s)),
                ScriptStuff.getHotkeyActionNames().stream().sorted().map(s -> new HotkeyTypeAndAction("script", s))
        ).collect(Collectors.toList());
    }

    /**
     * Returns an unmodifiable list of integers representing the windows virtual-key codes for the hotkey.
     *
     * @return an unmodifiable list of integers representing the windows virtual-key codes for the hotkey
     */
    public List<Integer> getKeys() {
        return this.keys;
    }

    /**
     * Like {@link #isPressed()} except only returns true at most once per key press.
     * Will only return true if the last key pressed was the main key.
     * Re-pressing modifier keys will not cause the method to return true.
     * The method will need to be called often to ensure intended results.
     *
     * @return true if called consistenly and follows the descriptions
     */
    public boolean wasPressed() {
        if (this.isPressed()) {
            if (!this.hasBeenPressed) {
                this.hasBeenPressed = true;
                return true;
            }
        } else {
            this.hasBeenPressed = this.isMainKeyPressed();
        }
        return false;
    }

    /**
     * Checks if the specified keys are pressed. If any extra modifier keys are pressed, the check will fail.
     * Extra keys that are not modifiers do not affect the check.
     *
     * @return true if the correct keys are pressed without any extra modifier keys
     */
    public boolean isPressed() {
        // If any keys belonging to the hotkey are not pressed, return false
        for (int vKey : this.keys) {
            if (!KeyboardUtil.isPressed(vKey)) {
                return false;
            }
        }

        // If any modifier keys that do not belong to the hotkey are pressed, return false
        for (int vKey : KeyboardUtil.SINGLE_MODIFIERS) {
            if (!this.keys.contains(vKey) && KeyboardUtil.isPressed(vKey)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if any non-modifier key is pressed. Hotkeys should be constructed with only one non-modifier
     * key for intended behaviour, so this should only be checking a single key.
     *
     * @return true if any non-modifier key is pressed
     */
    public boolean isMainKeyPressed() {
        for (int vKey : this.keys) {
            if (!KeyboardUtil.ALL_MODIFIERS.contains(vKey) && KeyboardUtil.isPressed(vKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        Hotkey hotkey = (Hotkey) o;

        return Objects.equals(this.keys, hotkey.keys);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int vKey : this.keys) {
            if (!out.toString().isEmpty()) {
                out.append(" + ");
            }
            out.append(KeyboardUtil.getKeyName(vKey));
        }
        return out.toString();
    }

    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    public int getMainKey() {
        for (int vKey : this.keys) {
            if (!KeyboardUtil.ALL_MODIFIERS.contains(vKey)) {
                return vKey;
            }
        }
        return -1;
    }

    /**
     * Like the regular Hotkey class, except ignores extra pressed modifier keys.
     */
    public static class HotkeyIM extends Hotkey {

        public HotkeyIM(List<Integer> keys) {
            super(keys);
        }

        @Override
        public boolean isPressed() {
            // If any keys belonging to the hotkey are not pressed, return false
            for (int vKey : this.keys) {
                if (!KeyboardUtil.isPressed(vKey)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class HotkeyTypeAndAction {
        public final String type;
        public final String action;

        public HotkeyTypeAndAction(String type, String action) {
            this.type = type;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            HotkeyTypeAndAction that = (HotkeyTypeAndAction) o;
            return Objects.equals(this.type, that.type) && Objects.equals(this.action, that.action);
        }

        @Override
        public String toString() {
            if (this.action.equalsIgnoreCase("none")) return "";
            return String.format("%s (%s)", Jingle.formatAction(this.action), StringUtils.capitalize(this.type));
        }
    }
}