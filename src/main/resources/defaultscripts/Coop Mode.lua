local ready = false
local screen_nil = false

local function is_number(value)
    return tonumber(value) ~= nil
end

local function on_main_instance_changed()
    ready = false
    screen_nil = false
end

local function customize()
    jingle.addCustomizationMenuCheckBox("cheats_enabled", true, "Enable cheats when opening to lan")
    jingle.addCustomizationMenuText(" ") -- intended way to add spacing
    jingle.addCustomizationMenuText("Delay (ms):")
    jingle.addCustomizationMenuTextField("delay", "50", is_number)
    jingle.showCustomizationMenu()
end


local function run()
    local delay = jingle.getCustomizable("delay", "50")
    jingle.sleep(tonumber(delay) or 50) -- ms
    jingle.openToLan(false, jingle.getCustomizable("cheats_enabled", "true") == "true")
    ready = false
end

-- Unlike in the Extra Keys script, we use both HERMES_STATE_CHANGE and HERMES_WORLD_LOG because handling out of order events is not that complicated here, and ends up being simpler.

local function on_hermes_state_change()
    screen_nil = hermes.getState()["screen"]["class"] == nil
    if not (ready and screen_nil) then
        return
    end
    run()
end


local function on_hermes_world_log()
    local entry = hermes.getWorldLogEntry()
    if entry["type"] == "entering" then
        ready = true
        if screen_nil then
            run()
        end
    elseif entry["type"] == "leave" then
        ready = false
    end
end

jingle.listen("HERMES_STATE_CHANGE", on_hermes_state_change)
jingle.listen("HERMES_WORLD_LOG", on_hermes_world_log)
jingle.listen("MAIN_INSTANCE_CHANGED", on_main_instance_changed)
jingle.setCustomization(customize)
