jingle_dir = os.getenv("UserProfile"):gsub("\\", "/") .. "/.config/Jingle/"

timers_activated = false
last_state = ''
last_projector_request = 'N'
total_width = 0
total_height = 0

mc_cap_name = "Jingle MC Capture" -- Switches to "Jingle MC Capture W" for window capture
using_win_cap = false             -- Switches to true for window capture
