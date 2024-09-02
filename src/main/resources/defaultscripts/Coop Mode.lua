should_run = -1

function on_world_enter()
    should_run = jingle.getCurrentTime() + 50
end

function check_should_run()
    if should_run ~= -1 and jingle.getCurrentTime() > should_run then
        should_run = -1
        jingle.openToLan(false, jingle.getCustomizable("cheats_enabled", "true") == "true")
    end
end

function customize()
    local ans = jingle.askYesNo("Should cheats be enabled when opening to lan?")
    if ans ~= nil then
        jingle.setCustomizable("cheats_enabled", ans and "true" or "false")
    end
end

jingle.listen("ENTER_WORLD", on_world_enter)
jingle.listen("END_TICK", check_should_run)
jingle.setCustomization(customize)
