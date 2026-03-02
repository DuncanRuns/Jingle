local function run_clear_worlds()
    if not jingle.isInstanceActive() then
        return
    end
    jingle.clearWorlds(false)
end

local initialized = false

local last_enter_world = 0
local world = nil
local world_loaded = false
local opened_to_lan = false
local screen_open = false
local screen_is_pause = false
local quick_reset_allowed = false

local function on_main_instance_changed()
    initialized = false
    last_enter_world = 0
    world = nil
    world_loaded = false
    opened_to_lan = false
    screen_open = false
    screen_is_pause = false
    quick_reset_allowed = false
end

local ends_with = basics.stringEndsWith

local function is_loading_screen(screen_class)
    if screen_class == nil then
        return false
    end
    -- Classes may need updating based on mc version
    return -- Fabric intermediary classes
        ends_with(screen_class, ".class_435") or
        ends_with(screen_class, ".class_3928") or
        ends_with(screen_class, ".class_434") or
        -- Official unobfuscated classes
        ends_with(screen_class, ".ProgressScreen") or
        ends_with(screen_class, ".LevelLoadingScreen") or
        ends_with(screen_class, ".ReceivingLevelScreen")
end

-- We could use HERMES_WORLD_LOG as well, but that might happen out of ideal order, and dealing with that could end up making this more complicated, and doesn't provide better functionality.
local function on_hermes_state_change()
    initialized = true
    local hermes_state = hermes.getState()
    local previous_world = world
    local new_world = hermes_state["world"]
    if new_world ~= nil then
        new_world = new_world["path"]
    end
    if previous_world ~= new_world then
        world_loaded = false
        world = new_world
    end
    local screen_class = hermes_state["screen"]["class"]
    screen_open = screen_class ~= nil
    screen_is_pause = hermes_state["screen"]["is_pause"]
    opened_to_lan = hermes_state["open_to_lan"] == true

    if world == nil or world_loaded then
        return
    end
    if not is_loading_screen(screen_class) then
        world_loaded = true
        quick_reset_allowed = true
        last_enter_world = jingle.getCurrentTime()
    end
end

local function get_reset_key()
    return jingle.getInstanceKeyOption("key_Create New World") or
        jingle.getInstanceKeyOption("key_Create New World§r")
end

local function can_run_preview_reset()
    return world ~= nil and not world_loaded and jingle.getCustomizable('p', 'false') == 'true'
end

local function can_run_reset()
    if not jingle.isInstanceActive() then
        return false
    end
    if not initialized then
        return false
    end

    if world ~= nil and world_loaded then
        if not screen_open then
            return jingle.getCustomizable('iwu', 'true') == 'true'
        end
        if screen_is_pause then
            return jingle.getCustomizable('iwp', 'true') == 'true'
        end
        if not screen_is_pause then
            return jingle.getCustomizable('iwgso', 'false') == 'true'
        end
    end

    if world == nil then
        return jingle.getCustomizable('t', 'false') == 'true'
    end

    return can_run_preview_reset()
end

local function press_reset()
    local key = get_reset_key()
    if key == nil then
        jingle.log("Can't reset! A create new world key is not set.")
        return
    end
    jingle.sendKeyToInstance(key)
end

local function run_safe_reset()
    if can_run_reset() then
        press_reset()
    end
end

local function run_quick_reset()
    if not can_run_reset() then
        return
    end
    if can_run_preview_reset() then
        press_reset()
        return
    end
    local time_limit = tonumber(jingle.getCustomizable('qr_time_limit', '20')) or 20
    if time_limit > 0 and math.abs(jingle.getCurrentTime() - last_enter_world) > (time_limit * 1000) then
        return
    end
    if quick_reset_allowed then
        press_reset()
    end
end

local function run_disable_quick_reset()
    quick_reset_allowed = false
end

local function run_start_coping()
    if not jingle.isInstanceActive() then
        return
    end
    if not world_loaded then
        return
    end
    if screen_open then
        return
    end
    if not opened_to_lan then
        jingle.openToLan(false, true)
    end
    jingle.sendChatMessage("/gamemode spectator") -- might fail if opened to lan with cheats disabled
end

local function run_minimize()
    if (jingle.isInstanceActive()) then
        jingle.minimizeInstance()
    end
end

local function is_number(value)
    return tonumber(value) ~= nil
end

local function customize()
    jingle.addCustomizationMenuText("Quick Reset Time Limit (set to 0 to disable time limit):")
    jingle.addCustomizationMenuTextField("qr_time_limit", "20", is_number)
    jingle.addCustomizationMenuText(" ")
    jingle.addCustomizationMenuText("Allowed states for 'Safe Reset' and 'Quick Reset':")
    jingle.addCustomizationMenuCheckBox("iwu", true, "In World, Unpaused")
    jingle.addCustomizationMenuCheckBox("iwp", true, "In World, Paused")
    jingle.addCustomizationMenuCheckBox("iwgso", false, "In World, Inventory/Chat Open")
    jingle.addCustomizationMenuCheckBox("t", false, "Title Screen")
    jingle.addCustomizationMenuCheckBox("p", false, "Previewing World")
    jingle.showCustomizationMenu()
end

jingle.addHotkey("Clear Worlds", run_clear_worlds)
jingle.listen("HERMES_STATE_CHANGE", on_hermes_state_change)
jingle.listen("MAIN_INSTANCE_CHANGED", on_main_instance_changed)
jingle.addHotkey("Safe Reset", run_safe_reset)
jingle.addHotkey("Quick Reset", run_quick_reset)
jingle.addHotkey("Disable Quick Reset", run_disable_quick_reset)
jingle.addHotkey("Start Coping", run_start_coping)
jingle.addHotkey("Minimize Instance", run_minimize)
jingle.setCustomization(customize)
