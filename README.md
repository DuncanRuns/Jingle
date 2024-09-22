# Jingle

An all-inclusive Minecraft speedrunning application written in **J**ava for a s**ingle** instance of Minecraft (As opposed to Julti which is written for M**ulti**).

Here's an overview of everything in Jingle sorted by the GUI tabs they can be found in.

- Jingle
    - The instance section at the top will become active when an instance is launched with the vanilla Minecraft launcher, MultiMC (and forks such as prism),  or ColorMC (chinese launcher).
        - `Clear Worlds` will clear all but the last 36 worlds, this is to ensure it doesn't clear anything important for file submission. most of those 36 worlds should be near empty and won't take up much space.
        - `Go Borderless` will set your game as a borderless window and fill the primary monitor.
        - `Package Submission Files` will automatically package worlds and logs needed for speedrun verification. This feature works best when used with SpeedRunIGT 14.0 or later!
        - `Open Minecraft Folder` will open the `.minecraft` directory of the instance.
    - Only a s**ingle** instance can be J**ingle**'s focus at any time, it will automatically switch to whatever instance you activate.
    - Basic Options
        - `Check for Updates` will enable checking for updates when launching Jingle, `Enable Pre-Release Updates` will make change it to include development versions.
        - `Minimize to Tray` will make it so hiding the Jingle window will also hide it on the task bar, putting it in the "tray" (the up arrow at the bottom left of the screen).
    - `Clear Worlds from All Instances` will do the action of `Clear Worlds` but for all instances that Jingle has ever seen.
    - `Open Jingle Folder` will open the folder containing configurations, save data, scripts, plugins, and more files for Jingle.
- Log
    - "A log is a file or record containing information about activities in a computer system" -lenovo.com
    - Each log line can come from Jingle itself, a plugin, or a script.
    - Select `Show Debug Logs` to see even more spam. Unlike Julti, this will show debug logs from the past as well!
- Hotkeys
    - An "action" or "hotkey action" is something that a set hotkey can do. Hotkey actions can be added by scripts, plugins, or Jingle itself. As of v1.0.0, the only hotkeys available are from scripts.
        - Hotkey actions from scripts are listed per script in the scripts section.
    - `Add` will let you add a new hotkey. You can select the desired hotkey action, set a keybind by pressing `Set Hotkey Here...`, and choose how modifier keys affect this key.
    - When hotkeys are added, the Hotkeys tab will then show the list of hotkeys with some related information and buttons.
        - `Action` is the hotkey action for that key.
        - `Hotkey` is the set key to run the action. A `*` symbol indicates that this key will ignore if Ctrl, Alt, or Shift is being pressed.
        - `Edit` will bring back the menu that was shown when originally adding the key, allowing you to change the key, action, or modifier behaviour.
        - `Remove` will immediately remove the key.
- Scripts
    - This tab shows a list of scripts. If a script ends with `.lua`, that means it has been manually added to the scripts folder, otherwise it is a script packaged with Jingle.
    - Each listed script has a few buttons.
        - `Customize` will show customization defined by a function in the script. It can be showing a whole menu of options, or asking a single question. Scripts are also able to not define any customization function, and the button will be greyed out.
        - `More...` will show a menu of extra buttons defined by the script. As of v1.0.0, the default scripts do not have any extra buttons.
        - `Enable`/`Disable` is for default scripts packaged with Jingle, since you can't remove them from the folder, you can disable them from running by pressing `Disable`, or bring back their functionality by pressing `Enable`.
    - `Open Scripts Folder` will open the folder where .lua files will be placed. The folder also contains a `libs` folder containing all the functions a script can use.
    - `Reload Scripts` will reload all scripts and load new ones added to the scripts folder.
    - As of v1.0.0 there are 3 default scripts packaged with Jingle.
        - **Coop Mode**: This script will automatically open to lan when joining a world. It will always do this if the script is enabled. Press `Customize` to decide if you want cheats enabled (for /difficulty and /time set 0).
        - **Misc**: This script adds 4 hotkeys actions, and some customization for 2 of them.
            - The customization for this script tweaks when the reset keys for this script are allowed to activate.
            - `Safe Reset` is a hotkey action that will reset the world for you, but only if you are in a location selected in the customization. **This will only work if you have a "Create New World" hotkey set in game, and it must be set to something different from this hotkey.**
            - `Reset Before 20s` is exactly like `Safe Reset`, except it will only work within the first 20 seconds of joining a world. This is useful for setting to a mouse button or another easily accessible button to make resetting more comfortable, but prevents accidentally pressing reset on a good run.
            - `Clear Worlds` is exactly like pressing the button from the `Jingle` tab.
            - `Start Coping` will open to lan with cheats enabled and send "/gamemode spectator" to the in game chat. **This will only work if you have an "Open Chat" key set in game.**
        - **Resizing**: This script adds 3 hotkey actions and customization for each.
            - `Eye Measuring` makes your Minecraft window really tall (and skinny to save on lag), this makes it so each pixel represents a tiny angle on screen, useful for eye measuring. This hotkey will also show and uncover the Eye Measuring Projector, see the OBS tab to get the Eye Measuring Projector.
            - `Planar Abuse` makes your Minecraft window really wide and short so that you can abuse planar fog to see further in the nether.
            - `Thin BT` makes your Minecraft window really skinny to provide a smaller scanning area for mapless, making it easier to distinguish where the buried treasure subchunk could be.
            - Customization:
                - Each of the window sizes for the hotkeys can be individually customized here.
                - `Undo Resizing after Reset` will enable automatic undoing of size changes after leaving the world. (You probably want to keep this on)
                - `Change Cursor Speed to 1 when Measuring` will set your windows cursor speed setting to 1 when activating the `Eye Measuring` hotkey action. This is only effective when used with raw input (in game setting) disabled.
                    - If enabled, another question will be asked, press `Yes` to the question if the windows mouse speed it asks about will always be your preferred windows mouse speed. If your preferred windows mouse speed ever changes, you can go through this customization again to save a new preferred speed.
- Plugins
    - The only thing this tab does is provide a space for plugins to add their own tabs for customization or buttons (or anything else that can be added to a GUI).
    - There are 2 default plugins as of Julti v1.0.0:
        - `PaceMan Tracker` tracks RSG Any% speedruns for [PaceMan.gg](https://paceman.gg/). Setup and further information can be found through the website.
        - `Standard Switcher` allows changing out the standard settings file for the game. [Standard Settings](https://github.com/KingContaria/StandardSettings/) is a mod that will reset your settings every time a new world is created. As of the release of SpeedrunAPI, all options can be customized in game, so no options will be shown within Jingle itself (Unlike Standard manager in Julti, which came before in game customization existed).
            - `Open Standard Switcher Folder` will open the folder containing created settings files. Deleting files should be done through this folder using Windows File Explorer.
            - `Create New File` will create a new standard settings file, copying the instance's current standard settings. It will then set the instance to use this file.
            - `Switch to Another File` will allow switching the instance's current standard settings to any created file in the Standard Switcher folder.
- OBS
    - This tab contains instructions on how to link OBS to Jingle. Follow the instructions in the tab to do so.
    - `Copy Script Path` will copy the full file path to the OBS script generated in the Jingle folder. This file will be updated by Jingle whenever Jingle updates and opens, so you should use this file path so that it can stay updated, and not a different copy of the script.
    - The following options will only work after following the instructions and linking Jingle to OBS.
        - `Enable Eye Meauring Projector` will automatically open an OBS projector for eye measuring, by default it should appear black until using a hotkey action that activates it. See the information in the scripts section about the eye measuring hotkey action.
        - `Automatically Position Eye Measuring Projector` will set a good widely used position for the projector, disabling this allows a specific custom position to be set.
        - `Projector Name Pattern` allows changing the window title searched for by Jingle for the Eye Measuring Projector. It is recommended to keep this on the default value `*- Jingle Mag`.
    - **The Eye Measuring Projector will not work if OBS is run in administrator mode while Jingle is not in administrator mode!**
- Donate
    - Should be a pretty self-explanatory tab. Thanks to everyone who has supported Julti and Jingle!

## Developing

Jingle GUIs are made with the IntelliJ IDEA form designer, if you intend on changing GUI portions of the code, IntelliJ IDEA must be configured in a certain way to ensure the GUI form works properly:
- `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle` -> `Build and run using: IntelliJ Idea`
- `Settings` -> `Editor` -> `GUI Designer` -> `Generate GUI into: Java source code`