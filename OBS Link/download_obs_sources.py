import requests
import os
import shutil


def try_download():
    remote_file = "https://raw.githubusercontent.com/bfxdev/OBS/master/scripting-dev/obslua.lua"
    local_filename = "obslua.lua"

    response = requests.get(remote_file)

    if response.status_code == 200:
        with open(local_filename, 'w') as file:
            file.write(response.text)
        print(
            f"File '{local_filename}' has been successfully downloaded to the working directory.")
        return True
    else:
        print(
            f"Failed to download the file. Status code: {response.status_code}")
        return False


def main():
    if os.path.exists("obslua.lua"):
        shutil.copy("obslua.lua", "obslua.lua.backup")

    if not try_download():
        shutil.copy("obslua.lua.backup", "obslua.lua")

    if os.path.isfile("obslua.lua.backup"):
        os.remove("obslua.lua.backup")


if __name__ == "__main__":
    main()
