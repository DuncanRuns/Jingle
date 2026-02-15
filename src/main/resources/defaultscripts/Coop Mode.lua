local initialized = false
local world_loaded = nil

local function is_number(value)
    return tonumber(value) ~= nil
end

local function on_main_instance_changed()
    initialized = false
    world_loaded = nil
end

local function customize()
    jingle.addCustomizationMenuCheckBox("cheats_enabled", true, "Enable cheats when opening to lan")
    jingle.addCustomizationMenuText(" ") -- intended way to add spacing
    jingle.addCustomizationMenuText("Delay (ms):")
    jingle.addCustomizationMenuTextField("delay", "50", is_number)
    jingle.showCustomizationMenu()
end

local function on_hermes_state_change()
    local hermes_state = hermes.getState()

    local new_world = hermes_state["world"]
    if new_world ~= nil then
        new_world = new_world["path"]
    end
    if not initialized or new_world == world_loaded then
        world_loaded = new_world
        initialized = true
        return
    end

    if (hermes_state["world"] == nil) or
        (hermes_state["screen"]["class"] ~= nil) or
        (hermes_state["open_to_lan"] == true) then
        return
    end
    world_loaded = new_world
    local delay = jingle.getCustomizable("delay", "50")
    jingle.sleep(tonumber(delay) or 50) -- ms
    jingle.openToLan(false, jingle.getCustomizable("cheats_enabled", "true") == "true")
end

jingle.listen("HERMES_STATE_CHANGE", on_hermes_state_change)
jingle.listen("MAIN_INSTANCE_CHANGED", on_main_instance_changed)
jingle.setCustomization(customize)
