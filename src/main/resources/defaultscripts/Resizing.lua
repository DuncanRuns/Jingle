function runThinBt()
    if (not jingle.isInstanceActive()) then
        return
    end
    checkUndoDirties()
    jingle.toggleResize(250, 750)
end

function runPlanarAbuse()
    if (not jingle.isInstanceActive()) then
        return
    end
    checkUndoCursor()
    jingle.toggleResize(1920, 300)
end

normal_cursor_speed = 0;
cursor_dirty = false
projector_dirty = false

function runEyeMeasuring()
    if (not jingle.isInstanceActive()) then
        return
    end
    if (jingle.toggleResize(384, 16384)) then
        jingle.bringOBSProjectorToTop()
        projector_dirty = true
        normal_cursor_speed = jingle.getCursorSpeed()
        jingle.setCursorSpeed(1)
        cursor_dirty = true
    else
        checkUndoDirties()
    end
end

function checkUndoDirties()
    if(projector_dirty) then
        jingle.dumpOBSProjector()
        projector_dirty = false
    end
    if (cursor_dirty) then
        jingle.setCursorSpeed(normal_cursor_speed)
        cursor_dirty = false
    end
end

function customize()
    jingle.log("TODO: implement customizing")
end

jingle.addHotkey("Thin BT", runThinBt)
jingle.addHotkey("Planar Abuse", runPlanarAbuse)
jingle.addHotkey("Eye Measuring", runEyeMeasuring)
jingle.setCustomization(customize)