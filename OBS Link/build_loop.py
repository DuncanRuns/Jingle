# This script is meant to re-build whenever any file changes in the src folder
# This helps with faster development

import os, time


def run_build():
    os.system("python build.py")


def get_time_list():
    out = []
    for i in os.listdir("src"):
        out.append(i + str(os.path.getmtime("src/" + i)))
    out.sort()
    return out


if __name__ == "__main__":
    old = []
    while True:
        time.sleep(0.1)
        new = get_time_list()
        if new != old:
            old = new
            run_build()
