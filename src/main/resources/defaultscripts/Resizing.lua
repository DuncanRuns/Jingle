function runThinBt()
    if (not jingle.isInstanceActive()) then
        return
    end
    jingle.toggleResize(250, 750)
end

function runPlanarAbuse()
    if (not jingle.isInstanceActive()) then
        return
    end
    jingle.toggleResize(1920, 300)
end

function runEyeMeasuring()
    if (not jingle.isInstanceActive()) then
        return
    end
    if (jingle.toggleResize(384, 16384)) then
        jingle.ensureOBSProjectorZ()
    else
        jingle.dumpOBSProjector()
    end
end

function customize()
    jingle.log("TODO: implement customizing")
end

jingle.addHotkey("Thin BT", runThinBt)
jingle.addHotkey("Planar Abuse", runPlanarAbuse)
jingle.addHotkey("Eye Measuring", runEyeMeasuring)
jingle.setCustomization(customize)