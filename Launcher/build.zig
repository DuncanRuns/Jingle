const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});

    const optimize = b.standardOptimizeOption(.{});

    const exe = b.addExecutable(.{
        .name = "Jingle Launcher",
        .root_source_file = b.path("jingle-launcher.zig"),
        .target = target,
        .optimize = optimize,
    });

    exe.addWin32ResourceFile(.{
        .file = b.path("icon.rc"),
    });
    exe.subsystem = .Windows;

    b.installArtifact(exe);
}
