--[[

    Jingle OBS Link v1.2.1
    
    The purpose of the OBS Link Script is to generate and control various Jingle related scenes and sources to assist in Speedrunning Minecraft.

    LICENSE BELOW:

    MIT License

    Copyright (c) 2024 DuncanRuns

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

]]

obs = obslua

---- Variables ----

jingle_dir = os.getenv("UserProfile"):gsub("\\", "/") .. "/.config/Jingle/"

timers_activated = false
last_state = ''
last_projector_request = 'N'
total_width = 0
total_height = 0

---- File Functions ----

function read_first_line(filename)
    local rfile = io.open(filename, "r")
    if rfile == nil then
        return ""
    end
    io.input(rfile)
    local out = io.read()
    io.close(rfile)
    return out
end

-- Don't think I'll need this
-- function write_file(filename, string)
--     local wfile = io.open(filename, "w")
--     io.output(wfile)
--     io.write(string)
--     io.close(wfile)
-- end

function get_state_file_string()
    local success, result = pcall(read_first_line, jingle_dir .. "obs-link-state")
    if success then
        return result
    end
    return nil
end

---- Misc Functions ----

function split_string(input_string, split_char)
    local out = {}
    -- https://stackoverflow.com/questions/1426954/split-string-in-lua
    for str in input_string.gmatch(input_string, "([^" .. split_char .. "]+)") do
        table.insert(out, str)
    end
    return out
end

---- Obs Functions ----

function get_scene(name)
    local source = get_source(name)
    if source == nil then
        return nil
    end
    local scene = obs.obs_scene_from_source(source)
    release_source(source)
    return scene
end

function get_group_as_scene(name)
    local source = get_source(name)
    if source == nil then
        return nil
    end
    local scene = obs.obs_group_from_source(source)
    release_source(source)
    return scene
end

function remove_source_or_scene(name)
    local source = get_source(name)
    obs.obs_source_remove(source)
    release_source(source)
end

--- Requires release after use
function get_source(name)
    return obs.obs_get_source_by_name(name)
end

function release_source(source)
    obs.obs_source_release(source)
end

function release_scene(scene)
    obs.obs_scene_release(scene)
end

function scene_exists(name)
    return get_scene(name) ~= nil
end

function create_scene(name)
    release_scene(obs.obs_scene_create(name))
end

function switch_to_scene(scene_name)
    local scene_source = get_source(scene_name)
    if (scene_source == nil) then return false end
    obs.obs_frontend_set_current_scene(scene_source)
    release_source(scene_source)
    return true
end

function get_video_info()
    local video_info = obs.obs_video_info()
    obs.obs_get_video_info(video_info)
    return video_info
end

function set_position_with_bounds(scene_item, x, y, width, height, center_align)
    -- default value false
    center_align = center_align or false

    local bounds = obs.vec2()
    bounds.x = width
    bounds.y = height

    if center_align then
        obs.obs_sceneitem_set_bounds_type(scene_item, obs.OBS_BOUNDS_NONE)
        local scale = obs.vec2()
        scale.x = center_align_scale_x
        scale.y = center_align_scale_y
        obs.obs_sceneitem_set_scale(scene_item, scale)
    else
        obs.obs_sceneitem_set_bounds_type(scene_item, obs.OBS_BOUNDS_STRETCH)
        obs.obs_sceneitem_set_bounds(scene_item, bounds)
    end

    -- set alignment of the scene item to: center_align ? CENTER : TOP_LEFT
    obs.obs_sceneitem_set_alignment(scene_item, center_align and 0 or 5)

    set_position(scene_item, x + (center_align and total_width / 2 or 0), y + (center_align and total_height / 2 or 0))
end

function set_position(scene_item, x, y)
    local pos = obs.vec2()
    pos.x = x
    pos.y = y
    obs.obs_sceneitem_set_pos(scene_item, pos)
end

function set_crop(scene_item, left, top, right, bottom)
    local crop = obs.obs_sceneitem_crop()
    crop.left = left
    crop.top = top
    crop.right = right
    crop.bottom = bottom
    obs.obs_sceneitem_set_crop(scene_item, crop)
end

function get_sceneitem_name(sceneitem)
    return obs.obs_source_get_name(obs.obs_sceneitem_get_source(sceneitem))
end

function bring_to_top(item)
    if item ~= nil then
        obs.obs_sceneitem_set_order(item, obs.OBS_ORDER_MOVE_TOP)
    end
end

function bring_to_bottom(item)
    obs.obs_sceneitem_set_order(item, obs.OBS_ORDER_MOVE_BOTTOM)
end

function delete_source(name)
    local source = get_source(name)
    if (source ~= nil) then
        obs.obs_source_remove(source)
        release_source(source)
    end
end

--- Returns true if created, otherwise false (scene will exist either way)
function ensure_scene_exists(name)
    if (false == scene_exists(name)) then
        create_scene(name)
        return true
    end
    return false
end

function get_active_scene_name()
    local current_scene_source = obs.obs_frontend_get_current_scene()
    local current_scene_name = obs.obs_source_get_name(current_scene_source)
    release_source(current_scene_source)
    return current_scene_name
end

---- Script Functions ----

-- TODO: Add loop that checks jingle state for walling vs playing and also open projector requests

function script_description()
    return
    "<h1>Jingle OBS Link</h1><p>Links OBS to Jingle.</p><p>Do not remove this script if you want automatic scene switching and projector opening.</p>"
end

function script_properties()
    local props = obs.obs_properties_create()

    obs.obs_properties_add_button(
        props, "generate_scenes_button", "Regenerate", regenerate)

    return props
end

function script_load()
    update_scene_size()
end

function script_update(settings)
    if timers_activated then
        return
    end

    timers_activated = true
    obs.timer_add(loop, 20)
    obs.timer_add(update_scene_size, 5000)
end

function update_scene_size()
    local video_info = get_video_info()

    if total_width ~= video_info.base_width or total_height ~= video_info.base_height then
        total_width = video_info.base_width
        total_height = video_info.base_height
    end
end

function regenerate()
    local game_cap = get_or_create_game_capture()
    local game_cap_pos = obs.vec2()
    game_cap_pos.x = total_width / 2
    game_cap_pos.y = total_height / 2

    if (ensure_scene_exists("Walling")) then
        local item = obs.obs_scene_add(get_scene("Walling"), game_cap)
        obs.obs_sceneitem_set_alignment(item, 0)
        obs.obs_sceneitem_set_pos(item, game_cap_pos)
    end

    if (ensure_scene_exists("Playing")) then
        local item = obs.obs_scene_add(get_scene("Playing"), game_cap)
        obs.obs_sceneitem_set_alignment(item, 0)
        obs.obs_sceneitem_set_pos(item, game_cap_pos)
    end

    if (ensure_scene_exists("Jingle Mag")) then
        local scene = get_scene("Jingle Mag")
        local cap_item = obs.obs_scene_add(scene, game_cap)
        obs.obs_sceneitem_set_bounds_type(cap_item, obs.OBS_BOUNDS_NONE)
        set_position(cap_item, total_width / 2, total_height / 2)
        obs.obs_sceneitem_set_alignment(cap_item, 0) -- align to center
        local scale = obs.vec2()
        scale.x = 32 * total_width / 1920
        scale.y = 2 * total_height / 1080
        obs.obs_sceneitem_set_scale(cap_item, scale)
        obs.obs_sceneitem_set_scale_filter(cap_item, obs.OBS_SCALE_POINT)

        local settings = obs.obs_data_create_from_json('{"file": "' .. jingle_dir .. '/measuring_overlay.png"}')
        local mag_source = obs.obs_source_create("image_source", "Measuring Overlay", settings, nil)
        obs.obs_data_release(settings)

        local mag_item = obs.obs_scene_add(scene, mag_source)
        obs.obs_sceneitem_set_scale_filter(mag_item, obs.OBS_SCALE_POINT)

        release_source(mag_source)

        local cover_source = get_or_create_cover()
        local cover_item = obs.obs_scene_add(scene, cover_source)

        set_position_with_bounds(cover_item, 0, 0, total_width, total_height, false)

        release_source(cover_source)
    end

    release_source(game_cap)
end

--- Make sure to use release_source() on it afterwards
-- Don't think I actually need this
-- function get_or_create_instance_1_group_source()
--     local group_source = get_source("Instance 1")
--     if (group_source ~= nil) then
--         release_source(group_source)
--         return group_source
--     end

--     group_source = obs.obs_source_create("group", "Instance 1", nil, nil)
--     local group = get_group_as_scene(group_source) -- no need to release

--     local game_cap = get_or_create_game_capture()
--     obs.obs_scene_add(group, game_cap)
--     release_source(game_cap)
--     return group_source
-- end

--- Make sure to use release_source() on it afterwards
function get_or_create_game_capture()
    local source = get_source("Minecraft Capture 1")
    if (source ~= nil) then
        return source
    end

    local settings = obs.obs_data_create_from_json(
        '{"capture_mode": "window","priority": 1,"window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}')

    source = obs.obs_source_create("game_capture", "Minecraft Capture 1", settings, nil)
    obs.obs_data_release(settings)

    return source
end

function get_or_create_cover()
    local source = get_source("Jingle Mag Cover")
    if source ~= nil then
        release_source(source)
    end

    local settings = obs.obs_data_create_from_json('{"color": 4278190080}')


    source = obs.obs_source_create("color_source", "Jingle Mag Cover", settings, nil)
    obs.obs_data_release(settings)

    return source
end

function loop()
    local state = get_state_file_string()

    if (state == last_state or state == nil) then
        return
    end

    last_state = state

    local state_args = split_string(state, '|')

    local current_scene_name = get_active_scene_name()

    if (#state_args == 0) then
        return;
    end

    local desired_scene = state_args[1]
    if desired_scene == 'P' and (current_scene_name == "Walling" or current_scene_name == "Jingle Mag") then
        switch_to_scene("Playing")
    end
    if desired_scene == 'W' and (current_scene_name == "Playing" or current_scene_name == "Jingle Mag") then
        switch_to_scene("Walling")
    end

    if #state_args == 1 then
        return;
    end

    local projector_request = state_args[2]
    if projector_request ~= last_projector_request then
        last_projector_request = projector_request
        if projector_request ~= 'N' and scene_exists("Jingle Mag") then
            obs.obs_frontend_open_projector("Scene", -1, "", "Jingle Mag")
        end
    end

    if #state_args == 2 then
        return;
    end

    local cover_reqest = state_args[3]
    local item = obs.obs_scene_find_source_recursive(get_scene("Jingle Mag"), "Jingle Mag Cover")
    if item ~= nil then
        obs.obs_sceneitem_set_visible(item, cover_reqest == 'Y')
    end
end

