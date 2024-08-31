function runThinBt()
    jingle.toggleResize(250, 750);
end
function runPlanarAbuse()
    jingle.toggleResize(1920, 300);
end

function customize()
    jingle.log("TODO: implement customizing")
end

function runDoom()
    jingle.log("No doom yet :(")
end

jingle.addHotkey("Thin BT", runThinBt)
jingle.addHotkey("Planar Abuse", runPlanarAbuse)
jingle.setCustomization(customize)

jingle.addExtraFunction("Run Doom", runDoom)