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
