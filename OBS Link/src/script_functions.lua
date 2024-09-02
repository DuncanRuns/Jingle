obs = obslua

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
        release_source(source)
        return source
    end

    local settings = obs.obs_data_create_from_json(
        '{"capture_mode": "window","priority": 1,"window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}')

    source = obs.obs_source_create("game_capture", "Minecraft Capture 1", settings, nil)
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
    if desired_scene == 'P' and current_scene_name == "Walling" then
        switch_to_scene("Playing")
    end
    if desired_scene == 'W' and current_scene_name == "Playing" then
        switch_to_scene("Playing")
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
end
