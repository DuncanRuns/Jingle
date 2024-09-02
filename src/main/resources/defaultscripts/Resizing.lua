function ask_size_customization(customize_name, message, default_width, default_height)
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
function get_size_customization(customize_name, default_width, default_height)
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
    eye_measuring_width, eye_measuring_height = get_size_customization("eye_measuring", 384, 16384)
    thin_bt_width, thin_bt_height = get_size_customization("thin_bt", 250, 750)
    planar_abuse_width, planar_abuse_height = get_size_customization("planar_abuse", 1920, 300)
    change_cursor_speed = jingle.getCustomizable("change_cursor_speed", tostring(false)) == "true"
    permanent_normal_cursor_speed = to_number_or_else(jingle.getCustomizable("permanent_normal_cursor_speed"), nil)
end

reload()

function should_run()
    return jingle.isInstanceActive() and (currently_resized or jingle.getInstanceState() == 'INWORLD')
end

function run_thin_bt()
    if not should_run() then
        return
    end
    check_undo_dirties()
    if jingle.toggleResize(thin_bt_width, thin_bt_height) then
        currently_resized = true
    else
        currently_resized = false
    end
end

function run_planar_abuse()
    if not should_run() then
        return
    end
    check_undo_dirties()
    if jingle.toggleResize(planar_abuse_width, planar_abuse_height) then
        currently_resized = true
    else
        currently_resized = false
    end
end

function run_eye_measuring()
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
        check_undo_dirties()
        currently_resized = false
    end
end

function check_undo_dirties()
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
    jingle.showMessageBox(
            "Customization for this script can be found in the \"More...\" button. Cursor customization is in \"Customize Eye Measuring\".")
end

function customize_eye_measuring()
    ask_size_customization("eye_measuring", "Enter your eye measuring size:", 384, 16384)
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

function customize_thin_bt()
    ask_size_customization("thin_bt", "Enter your thin bt size:", 250, 750)
end

function customize_planar_abuse()
    ask_size_customization("planar_abuse", "Enter your planar abuse size:", 1920, 300)
end

jingle.addHotkey("Thin BT", run_thin_bt)
jingle.addHotkey("Planar Abuse", run_planar_abuse)
jingle.addHotkey("Eye Measuring", run_eye_measuring)
jingle.listen("EXIT_WORLD", check_undo_dirties)
jingle.setCustomization(customize)
jingle.addExtraFunction("Customize Eye Measuring", customize_eye_measuring)
jingle.addExtraFunction("Customize Thin BT", customize_thin_bt)
jingle.addExtraFunction("Customize Planar Abuse", customize_planar_abuse)
