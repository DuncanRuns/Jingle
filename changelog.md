Changes in v1.1.0:

- Ported the EyeSee projector from @draconix6's Julti plugin to a new Jingle plugin, and added as a default plugin
  - Find it in Plugins -> EyeSee
  - In simpler terms, this adds the commonly requested zoom window with a ruler that was present in Julti, without needing OBS
  - As with Julti, it may not work with certain hardware configurations, a warning is included in the plugin tab
- Add "Minimize Projector When Inactive" option for the OBS Projector
- Added customizable borderless position (right-click the "Go Borderless" button)
- Some GUI tweaks/fixes

Changes in v1.0.0:

- Fix default cursor speed checkbox in Resizing customization script
- Better crash message for when trying to use as a fabric mod lol
- Rework OBS Link Script
    - Window capture support through secondary regenerate button
    - New name for mc capture
    - Regenerate button can now add missing mc capture
    - Julti support is now done by doing the following upon pressing either regenerate button:
        - Disable Julti source in Playing/Walling if it exists
        - Add Sound scene into Playing/Walling if it exists
        - Disable Minecraft Audio 1 in Sound scene if it exists
    - "Minecraft Capture 1" will also be disabled to not break with the previous Jingle format
- 🎉

Changes in v0.3.0:

- Added a "Safe Reset" key to the Misc script
    - Allows setting a key which won't reset the world when chat is open or any menus
        - Reset <20s also follows these rules
- Measuring projector will now also close when disabling the option in the OBS tab
- Bigly OBS Link Script updates
    - Added a "Fix Julti Game Cap" which will unbound, center align, and reset the scale of the Minecraft Capture from
      the Julti scene
        - I will probably remove this after like a year once SQ is available for more versions and there's no reason to
          have Julti styled scenes
    - Walling and Playing scenes will now also generate with an audio capture

Changes in v0.2.0:

- New script functionality for creating better customization menus
- Improved Resizing script customization with the new customization menus
- Move "Revert Window after Reset" option to the resizing script customization
- Close measuring projector(s) upon closing Jingle.

Changes in v0.1.x:

- Some plugin stuff (v0.1.8)
- Added Standard Switcher Plugin (v0.1.8)
- Fix measuring overlay not fitting to screen (v0.1.8)
- Fix projector name for non English languages (v0.1.7)
- Make projector name customizable (it already was I just forgot to add a box to the GUI) (v0.1.7)
- Request projector openings slower and slower as it continues to fail (v0.1.7)
- Fix undo resize crash fr (v0.1.6)
- Fix update suggestions while developing a plugin (v0.1.6)
- Fix some script hotkeys working when chat is open (v0.1.6)
- Fix undo resize crash for this guy named ravalle (v0.1.5)
- Fix scene switching (v0.1.5)
- Added MIT license (v0.1.5)
- Add a thingy for a plugin to use (v0.1.5)
- Fix some weird GUI things (v0.1.4)
- Added projector covers (v0.1.3)