obs = obslua

function script_description()
    return "<h1>Jingle OBS Link</h1><p>Links OBS to Jingle.</p>"
end

function script_properties()
    local props = obs.obs_properties_create()

    obs.obs_properties_add_button(
        props, "generate_scenes_button", "Regenerate", regenerate)

    return props
end

function regenerate()
    obs.script_log(200, "TODO: Implement")
end
