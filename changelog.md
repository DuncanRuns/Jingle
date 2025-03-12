Changes in v1.1.4:

- Fix Jingle umminimizing itself

Changes in v1.1.3:

- Allow many more key combinations (multiple main keys, or only modifier keys) ([#7](https://github.com/DuncanRuns/Jingle/issues/7), [#8](https://github.com/DuncanRuns/Jingle/issues/8))
- Allow plugins to specify a minimum Java version

Changes in v1.1.2:

- Fixed some unknown key names
- Added a new script library: `srigtevent`
- Fixed hotkeys conflicting with F3 ([#5](https://github.com/DuncanRuns/Jingle/issues/5))
    - If you bind a Jingle hotkey to `A`, then press `F3`+`A`, the Jingle hotkey is cancelled.
- Other small tweaks/fixes

Changes in v1.1.1:

- Updated PaceMan Tracker to v0.7.0

Changes in v1.1.0:

- Ported the EyeSee projector from @draconix6's Julti plugin to a new Jingle plugin, and added as a default plugin
    - Find it in Plugins -> EyeSee
    - In simpler terms, this adds the commonly requested zoom window with a ruler that was present in Julti, without
      needing OBS
    - As with Julti, it may not work with certain hardware configurations, a warning is included in the plugin tab
    - Improvements from @marin774
- Added Program Launcher Plugin (@joe-ldp)
- Major world bopping improvements, worlds should clear significantly faster
    - Do note that it will only log every 500 worlds cleared instead of every 50 now
- Added "Minimize Projector When Inactive" option for the OBS Projector
- Added customizable borderless position (right-click the "Go Borderless" button or go to options tab)
- Added Auto Borderless
- Added a "Quick Actions" section to the main Jingle tab
    - This area is for buttons that a user may need to use often enough such that it's annoying to sift through the tabs
      in Jingle
    - The new Program Launcher plugin adds a button here for launching the configured programs and/or Minecraft
    - The PaceMan Tracker plugin adds a button here for disabling/enabling the tracker (same with the AA tracker); the
      button will only appear if you have an access token entered
    - Buttons can be right-clicked to go to relevant configuration screens, as long as the plugin that added the button
      configures a right-click action
- Changed default Thin BT size to 280x1000
- Fixed update suggestion when downloading pre-release from GitHub
- Window titles (`Minecraft* - Instance 1`) will now revert after they are no longer the main instance on Jingle or if
  Jingle closes. This helps OBS capture the correct instance
- Moved "Basic Options" to a new options tab
- The size of the Jingle window will now be remembered when closing
- Fixed function to ask Windows for key names (@me-nx)
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