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
    return jingle.isInstanceActive() and
            ((jingle.getInstanceState() == "INWORLD" and jingle.getInstanceInWorldState() == "UNPAUSED")
                    or (jingle.getInstanceState() == "PREVIEWING"))
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

jingle.addHotkey("Clear Worlds", run_clear_worlds)
jingle.listen("ENTER_WORLD", save_enter_world_time)
jingle.addHotkey("Safe Reset", run_safe_reset)
jingle.addHotkey("Reset Before 20s", run_reset_before_20s)
jingle.addHotkey("Start Coping", run_start_coping)
