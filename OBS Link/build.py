import io
import os

from properties import *


def build(out_file: io.TextIOWrapper) -> None:
    print("Building...")
    print("Adding head comment...")
    out_file.write("--[[" + head_comment + "]]")
    out_file.write("\n\nobs = obslua\n\n")

    for lua_file_name in build_order:
        print("Adding " + lua_file_name + "...")
        out_file.write(get_src_file_string(lua_file_name))

    for lua_file_name in [i for i in sorted(os.listdir("src")) if (i not in build_order and i.endswith(".lua"))]:
        print("Adding " + lua_file_name + "...")
        out_file.write(get_src_file_string(lua_file_name))


def get_src_file_string(name: str) -> str:
    out = f"---- {get_display_name(name)} ----"
    with open("src/" + name, "r") as src_file:
        out += "\n\n" + get_actual_code(src_file.read()) + "\n\n"
    return out


def get_actual_code(code: str) -> str:
    out = ""
    for line in code.split("\n"):
        if line.strip().endswith("obslua"):
            continue
        out += line + "\n"
    return out.strip()


def get_display_name(name: str) -> str:
    if name.endswith(".lua"):
        name = name[:-4]
    out = ""
    for word in name.replace("_", " ").split(" "):
        out += word.capitalize() + " "
    return out.strip()


if __name__ == "__main__":
    with open(output_location, "w+") as out_file:
        build(out_file)
