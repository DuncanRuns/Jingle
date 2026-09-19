local last_world = nil

local function customize()
    local ans = jingle.askTextBox(
        "Enter the text to copy when entering a new world:",
        jingle.getCustomizable("text") or "",
        nil
    )

    if ans ~= nil then
        jingle.setCustomizable("text", ans)
    end
end

local function worlds_equal(world_a, world_b)
    if world_a == nil and world_b then return false end
    if world_b == nil and world_a then return false end
    return world_a["relative"] == world_b["relative"] and world_a["path"] == world_b["path"]
end

local function on_hermes_world_log()
    local entry = hermes.getWorldLogEntry()
    -- Check for valid entering entry
    if entry["type"] ~= "entering" then return end
    if not entry["world"] then return end

    -- Check for new world
    local world = entry["world"]
    if worlds_equal(world, last_world) then return end
    last_world = world

    local text = jingle.getCustomizable("text") or nil
    if text ~= nil then
        clipboard.set(text)
    end
end

jingle.listen("HERMES_WORLD_LOG", on_hermes_world_log)
jingle.setCustomization(customize)
