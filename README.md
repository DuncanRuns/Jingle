# Jingle

An all-inclusive Minecraft speedrunning application written in **J**ava for a s**ingle** instance of Minecraft (As
opposed to Julti which is written for M**ulti**).

Here's an overview of everything in Jingle sorted by the GUI tabs they can be found in.

### Jingle

- The instance section at the top will become active when an instance is launched with the vanilla Minecraft launcher,
  MultiMC (and forks such as prism), or ColorMC (chinese launcher).
    - `Clear Worlds` will clear all but the last 36 worlds, this is to ensure it doesn't clear anything important for
      file submission. most of those 36 worlds should be near empty and won't take up much space.
    - `Go Borderless` will set your game as a borderless window and fill the primary monitor. This button can be
      right-clicked for customization of the behaviour.
    - `Package Submission Files` will automatically package worlds and logs needed for speedrun verification. This
      feature works best when used with SpeedRunIGT 14.0 or later!
        - MCSR Fairplay also includes file packaging, this should be preferred in versions of the game supported by
          MCSR Fairplay.
    - `Open Minecraft Folder` will open the `.minecraft` directory of the instance.
- Only a s**ingle** instance can be J**ingle**'s focus at any time, it will automatically switch to whatever instance
  you focus.
- "Quick Actions" is a section for buttons added by plugins to do actions that are frequent enough to be featured on the
  main tab in Jingle but not frequent enough to need a hotkey.
    - Right-clicking quick action buttons may bring you to relevant configuration screens or plugin tabs.
- `Clear Worlds from All Instances` will do the action of `Clear Worlds` but for all instances that Jingle has ever
  seen.
- `Open Jingle Folder` will open the folder containing configurations, save data, scripts, plugins, and more files for
  Jingle.

## Options

- `Check for Updates` will enable checking for updates when launching Jingle, `Enable Pre-Release Updates` will change
  it to include development versions.
- `Minimize to Tray` will make it so hiding the Jingle window will also hide it on the task bar, putting it in the "
  tray" (the up arrow at the bottom left of the screen).
- `Customize Borderless` will let you customize borderless behaviour.
- `Auto Borderless` will automatically put detected instances into borderless 3 seconds after it is detected.

### Log

- "A log is a file or record containing information about activities in a computer system" -lenovo.com
- Each log line can come from Jingle itself, a plugin, or a script.
- `Upload Log` will upload the latest.log file to [MCLogs](https://mclogs.org/) and copy the URL to your clipboard.
- Select `Show Debug Logs` to see even more spam. Unlike Julti, this will show debug logs from the past as well!

### Hotkeys

- An "action" or "hotkey action" is something that a set hotkey can do. Hotkey actions can be added by scripts,
  plugins, or Jingle itself. As of v1.1.1, the only hotkeys available are from scripts.
    - Hotkey actions from scripts are listed per script in the scripts section.
- `Add` will let you add a new hotkey. You can select the desired hotkey action, set a keybind by pressing
  `Set Hotkey Here...`, and choose how modifier keys affect this key.
- When hotkeys are added, the Hotkeys tab will then show the list of hotkeys with some related information and
  buttons.
    - `Action` is the hotkey action for that key.
    - `Hotkey` is the set key to run the action. A `*` symbol indicates that this key will ignore if Ctrl, Alt, or
      Shift is being pressed.
    - `Edit` will bring back the menu that was shown when originally adding the key, allowing you to change the key,
      action, or modifier behaviour.
    - `Remove` will immediately remove the key.

### Scripts

- This tab shows a list of scripts. If a script ends with `.lua`, that means it has been manually added to the scripts
  folder, otherwise it is a script packaged with Jingle.
- Each listed script has a few buttons.
    - `Customize` will show customization defined by a function in the script. It can be showing a whole menu of
      options, or asking a single question. Scripts are also able to not define any customization function, and the
      button will be greyed out.
    - `More...` will show a menu of extra buttons defined by the script. As of v1.3.0, the default scripts do not have
      any extra buttons.
    - `Enable`/`Disable` is for default scripts packaged with Jingle, since you can't remove them from the folder, you
      can disable them from running by pressing `Disable`, or bring back their functionality by pressing `Enable`.
- `Open Scripts Folder` will open the folder where .lua files will be placed. The folder also contains a `libs` folder
  containing all the functions a script can use.
- `Reload Scripts` will reload all scripts and load new ones added to the scripts folder.
- As of v2.0, there are 2 default scripts packaged with Jingle.
    - **Coop Mode**: This script will automatically open to lan when joining a world. It will always do this if the
      script is enabled. Press `Customize` to decide if you want cheats enabled (for /difficulty and /time set 0), and
      change the delay before opening to lan.
    - **Extra Keys**: This script adds 6 hotkeys actions and some customization.
        - The customization for this script tweaks when the reset keys for this script are allowed to activate.
        - `Safe Reset` is a hotkey action that will reset the world for you, but only if you are in a location selected
          in the customization. **This will only work if you have a "Create New World" hotkey set in game, and it must
          be set to something different from this hotkey.**
        - `Quick Reset` is exactly like `Safe Reset`, except it will only work within the first 20 seconds of
          joining a world. This is useful for setting to a mouse button or another easily accessible button to make
          resetting more comfortable, but prevents accidentally pressing reset on a good run.
        - `Disable Quick Reset` will disable the `Quick Reset` hotkey action until the next world is loaded. For
          example, setting to left-click means once you start digging for a buried treasure, you can't accidentally
          reset using `Quick Reset`.
        - `Clear Worlds` is exactly like pressing the button from the `Jingle` tab.
        - `Start Coping` will open to lan with cheats enabled and send "/gamemode spectator" to the in game chat. **This
          will only work if you have an "Open Chat" key set in game.**
        - `Minimize Instance` will minimize the Minecraft window to the task bar. Works with borderless windows.

### Plugins

- The only thing this tab does is provide a space for plugins to add their own tabs for customization or buttons (or
  anything else that can be added to a GUI).
- There are 4 default plugins as of Jingle v2.0:
    - [`OBS Link`](https://github.com/DuncanRuns/Jingle-OBS-Link/) allows Jingle to detect whether you are on the wall
      or are in a game, and switch between "Playing" and "Walling" scenes.
    - [`PaceMan Tracker`](https://github.com/PaceMan-MCSR/PaceMan-Tracker/) tracks RSG Any% speedruns
      for [PaceMan.gg](https://paceman.gg/). Setup and further information can be found through the website.
    - [`Standard Switcher`](https://github.com/DuncanRuns/Jingle-Standard-Switcher) allows changing out the standard
      settings file for the game. [Standard Settings](https://github.com/KingContaria/StandardSettings/) is a mod that
      will reset your settings every time a new world is created. As of the release of SpeedrunAPI, all options can be
      customized in game, so no options will be shown within Jingle itself (Unlike Standard manager in Julti, which came
      before in game customization existed).
        - `Open Standard Switcher Folder` will open the folder containing created settings files. Deleting files should
          be done through this folder using Windows File Explorer.
        - `Create New File` will create a new standard settings file, copying the instance's current standard settings.
          It will then set the instance to use this file.
        - `Switch to Another File` will allow switching the instance's current standard settings to any created file in
          the Standard Switcher folder.
    - [`Program Launching`](https://github.com/joe-ldp/Jingle-Launch-Programs) allows adding a list of programs/files
      that are opened when pressing the launch button on the plugin tab or when using the quick action on the main
      Jingle tab. It also allows launching a Minecraft instance with Prism Launcher or MultiMC and automatically
      launching when Jingle launches.

### Community

- This tab contains a list of buttons that link to various community resources that have some relation to Jingle.
    - Retrieves buttons and links from
      [community.json](https://raw.githubusercontent.com/DuncanRuns/Jingle/refs/heads/meta/community.json).
- Includes a support section, with a button that links to [my ko-fi](https://ko-fi.com/DuncanRuns), and shows 3 random
  supporters from [supporters.txt](https://raw.githubusercontent.com/DuncanRuns/Jingle/refs/heads/meta/supporters.txt)
  every 5 seconds.

## Jingle Launcher

A small executable program that will run any Jingle jar it finds in the same folder. This allows running as admin and
pinning to the start menu.

[Download](https://github.com/DuncanRuns/Jingle/raw/refs/heads/v2/Launcher/zig-out/bin/Jingle%20Launcher.exe) | [Source Code](https://github.com/DuncanRuns/Jingle/tree/v2/Launcher)

## Developing

Jingle GUIs are made with the IntelliJ IDEA form designer, if you intend on changing GUI portions of the code, IntelliJ
IDEA must be configured in a certain way to ensure the GUI form works properly:

- `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle` -> `Build and run using: IntelliJ Idea`
- `Settings` -> `Editor` -> `GUI Designer` -> `Generate GUI into: Java source code`