function run_clear_worlds()
    if not jingle.isInstanceActive() then
        return
    end
    jingle.clearWorlds(false)
end

last_enter_world = 0

function save_enter_world_time()
    last_enter_world = jingle.getCurrentTime()
end

function get_reset_key()
    return jingle.getInstanceKeyOption("key_Create New World") or
            jingle.getInstanceKeyOption("key_Create New World§r")
end

function can_run_reset()
    if not jingle.isInstanceActive() then
        return false
    end

    state = jingle.getInstanceState()

    if state == 'INWORLD' then
        state = jingle.getInstanceInWorldState()
        if state == 'UNPAUSED' then
            return jingle.getCustomizable('iwu', 'true') == 'true'
        end
        if state == 'PAUSED' then
            return jingle.getCustomizable('iwp', 'true') == 'true'
        end
        if state == 'GAMESCREENOPEN' then
            return jingle.getCustomizable('iwgso', 'false') == 'true'
        end
    end

    if state == 'TITLE' then
        return jingle.getCustomizable('t', 'false') == 'true'
    end

    if state == 'PREVIEWING' then
        return jingle.getCustomizable('p', 'false') == 'true'
    end

    return false
end

function run_safe_reset()
    if can_run_reset() then
        local key = get_reset_key()
        if key == nil then
            jingle.log("Can't run Safe Reset! A create new world key is not set.")
            return
        end
        jingle.sendKeyToInstance(key)
    end
end

function run_reset_before_20s()
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

function run_start_coping()
    if (not jingle.isInstanceActive()) or (jingle.getInstanceState() ~= "INWORLD") or (jingle.getInstanceInWorldState() ~= "UNPAUSED") then
        return
    end
    jingle.openToLan(false, true)
    jingle.sendChatMessage("/gamemode spectator")
end

function customize()
    jingle.addCustomizationMenuText("Allowed states for 'Safe Reset' and 'Reset Before 20s':")
    jingle.addCustomizationMenuCheckBox("iwu", true, "In World, Unpaused")
    jingle.addCustomizationMenuCheckBox("iwp", true, "In World, Paused")
    jingle.addCustomizationMenuCheckBox("iwgso", false, "In World, Inventory/Chat Open")
    jingle.addCustomizationMenuCheckBox("t", false, "Title Screen")
    jingle.addCustomizationMenuCheckBox("p", false, "Previewing World")
    jingle.showCustomizationMenu()
end

jingle.addHotkey("Clear Worlds", run_clear_worlds)
jingle.listen("ENTER_WORLD", save_enter_world_time)
jingle.addHotkey("Safe Reset", run_safe_reset)
jingle.addHotkey("Reset Before 20s", run_reset_before_20s)
jingle.addHotkey("Start Coping", run_start_coping)
jingle.setCustomization(customize)