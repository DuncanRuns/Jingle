function split_string(input_string, split_char)
    local out = {}
    -- https://stackoverflow.com/questions/1426954/split-string-in-lua
    for str in input_string.gmatch(input_string, "([^" .. split_char .. "]+)") do
        table.insert(out, str)
    end
    return out
end
