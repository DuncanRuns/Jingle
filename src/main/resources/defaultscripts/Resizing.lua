function askSizeCustomization(customize_name, message, default_width, default_height)
    local ans = jingle.askTextBox(
            message,
            jingle.getCustomizable(customize_name, tostring(default_width) .. "x" .. tostring(default_height)),
            is_size_string
    )
    if ans ~= nil then
        jingle.setCustomizable(customize_name, ans)
        reload()
    end
end

---@param customize_name string
---@param default_width integer
---@param default_height integer
function getSizeCustomization(customize_name, default_width, default_height)
    local stored_val = jingle.getCustomizable(customize_name, nil)
    if stored_val ~= nil and is_size_string(stored_val) then
        return size_string_to_numbers(stored_val)
    end
    return default_width, default_height
end

---@param size_string string
function size_string_to_numbers(size_string)
    return size_string:match("(%d+)x(%d+)")
end

function to_number_or_else(input, def)
    if input == nil then
        return def
    end
    local out = tonumber(input)
    if out == nil then
        return def
    end
    return out
end

function is_size_string(input)
    return nil ~= input:match("^%d+x%d+$")
end

normal_cursor_speed = 0
cursor_dirty = false
projector_dirty = false
currently_resized = false

eye_measuring_width, eye_measuring_height = nil, nil
thin_bt_width, thin_bt_height = nil, nil
planar_abuse_width, planar_abuse_height = nil, nil
change_cursor_speed = nil
permanent_normal_cursor_speed = nil

function reload()
    eye_measuring_width, eye_measuring_height = getSizeCustomization("eye_measuring", 384, 16384)
    thin_bt_width, thin_bt_height = getSizeCustomization("thin_bt", 250, 750)
    planar_abuse_width, planar_abuse_height = getSizeCustomization("planar_abuse", 1920, 300)
    change_cursor_speed = jingle.getCustomizable("change_cursor_speed", tostring(false)) == "true"
    permanent_normal_cursor_speed = to_number_or_else(jingle.getCustomizable("permanent_normal_cursor_speed"), nil)
end

reload()

function should_run()
    return jingle.isInstanceActive() and (currently_resized or jingle.getInstanceState() == 'INWORLD')
end

function runThinBt()
    if not should_run() then
        return
    end
    checkUndoDirties()
    if jingle.toggleResize(thin_bt_width, thin_bt_height) then
        currently_resized = true
    else
        currently_resized = false
    end
end

function runPlanarAbuse()
    if not should_run() then
        return
    end
    checkUndoDirties()
    if jingle.toggleResize(planar_abuse_width, planar_abuse_height) then
        currently_resized = true
    else
        currently_resized = false
    end
end

function runEyeMeasuring()
    if not should_run() then
        return
    end
    if jingle.toggleResize(eye_measuring_width, eye_measuring_height) then
        jingle.bringOBSProjectorToTop()
        projector_dirty = true
        normal_cursor_speed = jingle.getCursorSpeed()
        if change_cursor_speed then
            jingle.setCursorSpeed(1)
            cursor_dirty = true
        end
        currently_resized = true
    else
        checkUndoDirties()
        currently_resized = false
    end
end

function checkUndoDirties()
    if projector_dirty then
        jingle.dumpOBSProjector()
        projector_dirty = false
    end
    if cursor_dirty then
        jingle.setCursorSpeed(permanent_normal_cursor_speed or normal_cursor_speed)
        cursor_dirty = false
    end
end

function customize()
    askSizeCustomization("eye_measuring", "Enter your eye measuring size (or cancel to skip):", 384, 16384)
    askSizeCustomization("thin_bt", "Enter your thin bt size (or cancel to skip):", 250, 750)
    askSizeCustomization("planar_abuse", "Enter your planar abuse size (or cancel to skip):", 1920, 300)
    local ans = jingle.askYesNo("Set cursor speed to 1 while eye measuring?")
    if (ans ~= nil) then
        if (ans) then
            jingle.setCustomizable("change_cursor_speed", "true")
        else
            jingle.setCustomizable("change_cursor_speed", "false")
        end
        reload()
        if ans then
            local current_cursor_speed = tostring(jingle.getCursorSpeed())
            local ans = jingle.askYesNo("Revert cursor speed to " ..
                    current_cursor_speed .. " after undoing eye measuring?")
            if ans ~= nil then
                if ans then
                    jingle.setCustomizable("permanent_normal_cursor_speed", current_cursor_speed)
                else
                    jingle.setCustomizable("permanent_normal_cursor_speed", nil)
                end
            end
        end
    end
end

jingle.addHotkey("Thin BT", runThinBt)
jingle.addHotkey("Planar Abuse", runPlanarAbuse)
jingle.addHotkey("Eye Measuring", runEyeMeasuring)
jingle.listen("EXIT_WORLD", checkUndoDirties)
jingle.setCustomization(customize)
