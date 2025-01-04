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
    thin_bt_width, thin_bt_height = get_size_customization("thin_bt", 280, 1000)
    planar_abuse_width, planar_abuse_height = get_size_customization("planar_abuse", 1920, 300)
    change_cursor_speed = jingle.getCustomizable("change_cursor_speed", tostring(false)) == "true"
    permanent_normal_cursor_speed = to_number_or_else(jingle.getCustomizable("permanent_normal_cursor_speed"), nil)
    undo_resize_on_reset = jingle.getCustomizable("undo_resize_on_reset", "true") == "true"
end

reload()

function should_run()
    if not jingle.isInstanceActive() then
        return false
    end
    return currently_resized or
            (not jingle.hasFabricMod('state-output')) or
            (jingle.getInstanceState() == 'INWORLD' and jingle.getInstanceInWorldState() ~= 'GAMESCREENOPEN')
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
        jingle.showMeasuringProjector()
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

function on_exit_world()
    if currently_resized and undo_resize_on_reset then
        jingle.undoResize()
        currently_resized = false
    end
    check_undo_dirties()
end

function check_undo_dirties()
    if projector_dirty then
        jingle.dumpMeasuringProjector()
        projector_dirty = false
    end
    if cursor_dirty then
        jingle.setCursorSpeed(permanent_normal_cursor_speed or normal_cursor_speed)
        cursor_dirty = false
    end
end

function customize()
    jingle.addCustomizationMenuCheckBox("undo_resize_on_reset", true, "Undo Resizing after Reset")
    jingle.addCustomizationMenuText(" ")
    jingle.addCustomizationMenuText("Enter your eye measuring size:")
    jingle.addCustomizationMenuTextField("eye_measuring", "384x16384", is_size_string)
    jingle.addCustomizationMenuCheckBox("change_cursor_speed", false, "Change Cursor Speed to 1 when Measuring")
    jingle.addCustomizationMenuText(" ")
    jingle.addCustomizationMenuText("Enter your thin bt size:")
    jingle.addCustomizationMenuTextField("thin_bt", "280x1000", is_size_string)
    jingle.addCustomizationMenuText(" ")
    jingle.addCustomizationMenuText("Enter your planar abuse size:")
    jingle.addCustomizationMenuTextField("planar_abuse", "1920x300", is_size_string)

    if not jingle.showCustomizationMenu() then
        return
    end

    if jingle.getCustomizable("change_cursor_speed", tostring(false)) ~= "true" then
        reload()
        return
    end

    local current_cursor_speed = tostring(jingle.getCursorSpeed())
    local ans = jingle.askYesNo("Always revert cursor speed to " ..
            current_cursor_speed .. " after undoing eye measuring? (Otherwise revert to the speed from before measuring)")
    if ans ~= nil then
        if ans then
            jingle.setCustomizable("permanent_normal_cursor_speed", current_cursor_speed)
        else
            jingle.setCustomizable("permanent_normal_cursor_speed", nil)
        end
    end

    reload();
end

jingle.addHotkey("Thin BT", run_thin_bt)
jingle.addHotkey("Planar Abuse", run_planar_abuse)
jingle.addHotkey("Eye Measuring", run_eye_measuring)
jingle.listen("EXIT_WORLD", on_exit_world)
jingle.setCustomization(customize)
