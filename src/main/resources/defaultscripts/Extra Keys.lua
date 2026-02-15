local function run_clear_worlds()
    if not jingle.isInstanceActive() then
        return
    end
    jingle.clearWorlds(false)
end

local last_enter_world = 0
local world = nil
local world_loaded = false
local opened_to_lan = false
local screen_open = false
local screen_is_pause = false

local function on_main_instance_changed()
    last_enter_world = 0
    world = nil
    world_loaded = false
    opened_to_lan = false
    screen_open = false
    screen_is_pause = false
end

local function on_hermes_state_change()
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
    if world ~= nil and world_loaded == false then
        if hermes_state["screen"]["class"] == nil then
            world_loaded = true
            last_enter_world = jingle.getCurrentTime()
        elseif basics.stringEndsWith(hermes_state["screen"]["class"], ".class_433") then -- fabric intermediary
            world_loaded = true
            last_enter_world = jingle.getCurrentTime()
        elseif basics.stringEndsWith(hermes_state["screen"]["class"], ".PauseScreen") then -- unobfuscated 1.21.11+
            world_loaded = true
            last_enter_world = jingle.getCurrentTime()
        end
    end

    screen_open = hermes_state["screen"]["class"] ~= nil
    screen_is_pause = hermes_state["screen"]["is_pause"]
    opened_to_lan = hermes_state["opened_to_lan"] == true
end

local function get_reset_key()
    return jingle.getInstanceKeyOption("key_Create New World") or
        jingle.getInstanceKeyOption("key_Create New World§r")
end

local function can_run_reset()
    if not jingle.isInstanceActive() then
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

    if world ~= nil and not world_loaded then
        return jingle.getCustomizable('p', 'false') == 'true'
    end

    return false
end

local function run_safe_reset()
    if can_run_reset() then
        local key = get_reset_key()
        if key == nil then
            jingle.log("Can't run Safe Reset! A create new world key is not set.")
            return
        end
        jingle.sendKeyToInstance(key)
    end
end

local function run_reset_before_20s()
    if not can_run_reset() then
        return
    end
    if math.abs(jingle.getCurrentTime() - last_enter_world) > 20000 then
        return
    end
    local key = get_reset_key()
    if key == nil then
        jingle.log("Can't run Reset Before 20s! A create new world key is not set.")
        return
    end
    jingle.sendKeyToInstance(key)
end

local function run_start_coping()
    if not jingle.isInstanceActive() then
        return
    end
    if opened_to_lan then
        return
    end
    if not world_loaded then
        return
    end
    if screen_open then
        return
    end
    jingle.openToLan(false, true)
    jingle.sendChatMessage("/gamemode spectator")
    opened_to_lan = true
end

local function run_minimize()
    if (jingle.isInstanceActive()) then
        jingle.minimizeInstance()
    end
end

local function customize()
    jingle.addCustomizationMenuText("Allowed states for 'Safe Reset' and 'Reset Before 20s':")
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
jingle.addHotkey("Reset Before 20s", run_reset_before_20s)
jingle.addHotkey("Start Coping", run_start_coping)
jingle.addHotkey("Minimize Instance", run_minimize)
jingle.setCustomization(customize)
