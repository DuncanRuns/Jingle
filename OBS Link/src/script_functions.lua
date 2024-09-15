obs = obslua

-- TODO: Add loop that checks jingle state for walling vs playing and also open projector requests

function script_description()
    return
    [[
    <h1>Jingle OBS Link</h1>
    <p>Links OBS to Jingle.</p>
    <p>Do not remove this script if you want automatic scene switching and projector opening.</p>
    <h2>Press "Regenerate with 'Game Capture'" if you use Minecraft's built-in fullscreen. Otherwise use Window Capture.</h2>
    ]]
end

function script_properties()
    local props = obs.obs_properties_create()

    obs.obs_properties_add_button(
        props, "regenerate_gc_button", "Regenerate With 'Game Capture'", regenerate_gc)
    obs.obs_properties_add_button(
        props, "regenerate_wc_button", "Regenerate With 'Window Capture'", regenerate_wc)

    return props
end

function script_load()
    update_scene_size()
    last_state = get_state_file_string()
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

function regenerate_gc()
    using_win_cap = false
    mc_cap_name = "Jingle MC Capture"
    regenerate()
end

function regenerate_wc()
    using_win_cap = true
    mc_cap_name = "Jingle MC Capture W"
    regenerate()
end

function regenerate()
    local mc_cap = get_or_create_mc_capture()
    local audio_cap = get_or_create_audio_capture()
    local mc_cap_pos = obs.vec2()
    mc_cap_pos.x = total_width / 2
    mc_cap_pos.y = total_height / 2

    setup_recording_scene("Walling", mc_cap, audio_cap, mc_cap_pos)
    setup_recording_scene("Playing", mc_cap, audio_cap, mc_cap_pos)

    if (ensure_scene_exists("Jingle Mag")) then
        local scene = get_scene("Jingle Mag")

        local settings = obs.obs_data_create_from_json('{"file": "' .. jingle_dir .. '/measuring_overlay.png"}')
        local mag_source = obs.obs_source_create("image_source", "Measuring Overlay", settings, nil)
        obs.obs_data_release(settings)

        local mag_item = obs.obs_scene_add(scene, mag_source)
        obs.obs_sceneitem_set_scale_filter(mag_item, obs.OBS_SCALE_POINT)
        set_position_with_bounds(mag_item, 0, 0, total_width, total_height, false)

        release_source(mag_source)

        local cover_source = get_or_create_cover()
        local cover_item = obs.obs_scene_add(scene, cover_source)

        set_position_with_bounds(cover_item, 0, 0, total_width, total_height, false)

        release_source(cover_source)
    end
    setup_jingle_mag_mc_cap(mc_cap)

    release_source(mc_cap)
    release_source(audio_cap)
    set_item_visible("Playing", "Jingle MC Capture", not using_win_cap)
    set_item_visible("Walling", "Jingle MC Capture", not using_win_cap)
    set_item_visible("Jingle Mag", "Jingle MC Capture", not using_win_cap)
    set_item_visible("Playing", "Jingle MC Capture W", using_win_cap)
    set_item_visible("Walling", "Jingle MC Capture W", using_win_cap)
    set_item_visible("Jingle Mag", "Jingle MC Capture W", using_win_cap)
    set_item_visible("Playing", "Minecraft Capture 1", false)
    set_item_visible("Walling", "Minecraft Capture 1", false)
    set_item_visible("Jingle Mag", "Minecraft Capture 1", false)
    set_item_visible("Playing", "Julti", false)
    set_item_visible("Walling", "Julti", false)
    set_item_visible("Sound", "Minecraft Audio 1", false)
end

function setup_jingle_mag_mc_cap(mc_cap)
    local scene = get_scene("Jingle Mag")
    if obs.obs_scene_find_source_recursive(scene, mc_cap_name) ~= nil then
        return
    end

    local cap_item = bring_to_bottom(obs.obs_scene_add(scene, mc_cap))
    obs.obs_sceneitem_set_bounds_type(cap_item, obs.OBS_BOUNDS_NONE)
    set_position(cap_item, total_width / 2, total_height / 2)
    obs.obs_sceneitem_set_alignment(cap_item, 0) -- align to center
    local scale = obs.vec2()
    scale.x = 32 * total_width / 1920
    scale.y = 2 * total_height / 1080
    obs.obs_sceneitem_set_scale(cap_item, scale)
    obs.obs_sceneitem_set_scale_filter(cap_item, obs.OBS_SCALE_POINT)
end

function setup_recording_scene(scene_name, mc_cap, audio_cap, mc_pos)
    if (ensure_scene_exists(scene_name)) then
        local scene = get_scene(scene_name)
        local item = obs.obs_scene_add(scene, mc_cap)
        obs.obs_sceneitem_set_alignment(item, 0)
        obs.obs_sceneitem_set_pos(item, mc_pos)
        obs.obs_scene_add(scene, audio_cap)
        local sound_source = get_source("Sound")
        if sound_source then
            bring_to_bottom(obs.obs_scene_add(scene, sound_source))
            release_source(sound_source)
        end
        return
    end
    local scene = get_scene(scene_name)
    if obs.obs_scene_find_source_recursive(scene, "Sound") == nil then
        local sound_source = get_source("Sound")
        if sound_source then
            bring_to_bottom(obs.obs_scene_add(scene, sound_source))
            release_source(sound_source)
        end
    end
    if obs.obs_scene_find_source_recursive(scene, mc_cap_name) == nil then
        local item = bring_to_bottom(obs.obs_scene_add(scene, mc_cap))
        obs.obs_sceneitem_set_alignment(item, 0)
        obs.obs_sceneitem_set_pos(item, mc_pos)
    end
    if obs.obs_scene_find_source_recursive(scene, "Jingle MC Audio") == nil then
        bring_to_bottom(obs.obs_scene_add(scene, audio_cap))
    end
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

--     local game_cap = get_or_create_mc_capture()
--     obs.obs_scene_add(group, game_cap)
--     release_source(game_cap)
--     return group_source
-- end

--- Make sure to use release_source() on it afterwards
function get_or_create_mc_capture()
    local source = get_source(mc_cap_name)
    if (source ~= nil) then
        return source
    end

    local settings = nil
    if using_win_cap then
        settings = obs.obs_data_create_from_json('{"priority": 1, "window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}')
    else
        settings = obs.obs_data_create_from_json(
            '{"capture_mode": "window","priority": 1,"window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}')
    end

    if using_win_cap then
        source = obs.obs_source_create("window_capture", mc_cap_name, settings, nil)
    else
        source = obs.obs_source_create("game_capture", mc_cap_name, settings, nil)
    end
    obs.obs_data_release(settings)

    return source
end

--- Make sure to use release_source() on it afterwards
function get_or_create_audio_capture()
    local source = get_source("Jingle MC Audio")
    if (source ~= nil) then
        return source
    end

    local settings = obs.obs_data_create_from_json(
        '{"priority": 1,"window": "Minecraft* - Instance 1:GLFW30:javaw.exe"}')

    source = obs.obs_source_create("wasapi_process_output_capture", "Jingle MC Audio", settings, nil)
    obs.obs_data_release(settings)

    return source
end

--- Make sure to use release_source() on it afterwards
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
