const std = @import("std");

pub fn main() !void {
    var arena = std.heap.ArenaAllocator.init(std.heap.page_allocator);
    defer arena.deinit();
    const allocator = arena.allocator();

    const dir_it_allowed = (try std.fs.cwd().openDir("./", .{ .iterate = true }));
    var it = dir_it_allowed.iterate();

    while (try it.next()) |val| {
        if (val.kind != .file) continue;
        const filename = val.name;
        if (!(std.mem.startsWith(u8, filename, "Jingle") and std.mem.endsWith(u8, filename, ".jar"))) continue;
        const argv = [_][]const u8{ "javaw", "-jar", val.name };
        _ = try std.process.Child.run(.{
            .allocator = allocator,
            .argv = &argv,
        });
        break;
    }
}
