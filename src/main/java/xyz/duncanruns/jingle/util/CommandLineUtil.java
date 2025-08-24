package xyz.duncanruns.jingle.util;

import com.sun.jna.*;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;
import org.apache.commons.exec.CommandLine;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility for reading command line arguments of another process by PID.
 * Based on ProcEnvUtil but reads command line instead of environment variables.
 */
public class CommandLineUtil {

    // NtQueryInformationProcess constant
    private static final int ProcessBasicInformation = 0;

    // Offsets for 64-bit Windows 10+
    private static final int PEB_OFFSET_ProcessParameters = 0x20;
    private static final int RTL_USER_PROCESS_PARAMETERS_OFFSET_CommandLine = 0x70;

    // Max command line size (32 KB should be enough)
    private static final int MAX_CMDLINE_SIZE = 32 * 1024;

    // Structure for PROCESS_BASIC_INFORMATION (simplified)
    public static class PROCESS_BASIC_INFORMATION extends Structure {
        public Pointer Reserved1;
        public Pointer PebBaseAddress;
        public Pointer Reserved2_0;
        public Pointer Reserved2_1;
        public Pointer UniqueProcessId;
        public Pointer Reserved3;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "Reserved1",
                    "PebBaseAddress",
                    "Reserved2_0",
                    "Reserved2_1",
                    "UniqueProcessId",
                    "Reserved3"
            );
        }
    }

    // UNICODE_STRING structure
    public static class UNICODE_STRING extends Structure {
        public short Length;
        public short MaximumLength;
        public Pointer Buffer;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("Length", "MaximumLength", "Buffer");
        }
    }

    // Ntdll interface
    public interface Ntdll extends Library {
        Ntdll INSTANCE = Native.load("Ntdll", Ntdll.class, W32APIOptions.UNICODE_OPTIONS);

        int NtQueryInformationProcess(HANDLE processHandle,
                                      int processInformationClass,
                                      Structure processInformation,
                                      int processInformationLength,
                                      IntByReference returnLength);
    }

    /**
     * Reads the command line of another process by PID.
     */
    public static String getCommandLineStringFromPid(int pid) throws Exception {
        HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false,
                pid
        );
        if (process == null) {
            throw new IllegalStateException("Failed to open process " + pid + ": " +
                    Kernel32.INSTANCE.GetLastError());
        }

        try {
            // Query basic info (gives us PEB address)
            PROCESS_BASIC_INFORMATION pbi = new PROCESS_BASIC_INFORMATION();
            IntByReference retLen = new IntByReference();

            int status = Ntdll.INSTANCE.NtQueryInformationProcess(
                    process,
                    ProcessBasicInformation,
                    pbi,
                    pbi.size(),
                    retLen
            );
            if (status != 0) {
                throw new IllegalStateException("NtQueryInformationProcess failed: " + status);
            }

            Pointer pebAddress = pbi.PebBaseAddress;

            // Read pointer to ProcessParameters from PEB
            Pointer processParametersAddress = readPointer(process, pebAddress.share(PEB_OFFSET_ProcessParameters));

            // Read UNICODE_STRING structure for CommandLine
            UNICODE_STRING cmdLineUnicodeString = new UNICODE_STRING();
            IntByReference bytesRead = new IntByReference();

            boolean ok = Kernel32.INSTANCE.ReadProcessMemory(
                    process,
                    processParametersAddress.share(RTL_USER_PROCESS_PARAMETERS_OFFSET_CommandLine),
                    cmdLineUnicodeString.getPointer(),
                    cmdLineUnicodeString.size(),
                    bytesRead
            );
            if (!ok) {
                throw new IllegalStateException("ReadProcessMemory (UNICODE_STRING) failed: " +
                        Kernel32.INSTANCE.GetLastError());
            }
            cmdLineUnicodeString.read();

            // Read the actual command line string
            if (cmdLineUnicodeString.Buffer == null || cmdLineUnicodeString.Length <= 0) {
                return "";
            }

            Memory buffer = new Memory(Math.min(cmdLineUnicodeString.Length, MAX_CMDLINE_SIZE));
            ok = Kernel32.INSTANCE.ReadProcessMemory(
                    process,
                    cmdLineUnicodeString.Buffer,
                    buffer,
                    (int) buffer.size(),
                    bytesRead
            );
            if (!ok) {
                throw new IllegalStateException("ReadProcessMemory (command line) failed: " +
                        Kernel32.INSTANCE.GetLastError());
            }

            byte[] data = buffer.getByteArray(0, bytesRead.getValue());
            return new String(data, StandardCharsets.UTF_16LE);

        } finally {
            Kernel32.INSTANCE.CloseHandle(process);
        }
    }

    /**
     * Helper to read a pointer-sized value from another process.
     */
    private static Pointer readPointer(HANDLE process, Pointer address) {
        Memory buffer = new Memory(Native.POINTER_SIZE);
        IntByReference bytesRead = new IntByReference();

        boolean ok = Kernel32.INSTANCE.ReadProcessMemory(
                process,
                address,
                buffer,
                (int) buffer.size(),
                bytesRead
        );

        if (!ok || bytesRead.getValue() != buffer.size()) {
            throw new IllegalStateException("ReadProcessMemory (pointer) failed: " +
                    Kernel32.INSTANCE.GetLastError());
        }

        long value = (Native.POINTER_SIZE == 8)
                ? buffer.getLong(0)
                : buffer.getInt(0) & 0xFFFFFFFFL;

        return new Pointer(value);
    }

    public static Map<String, String> extractOptions(List<String> args) {
        Map<String, String> outOptions = new HashMap<>();
        String keyToSet = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                keyToSet = null;
                while (arg.startsWith("-")) arg = arg.substring(1);
                int equals = arg.indexOf("=");
                if (equals == -1) keyToSet = arg;
                else outOptions.put(arg.substring(0, equals), arg.substring(equals + 1));
            } else if (keyToSet != null) {
                outOptions.put(keyToSet, arg);
                keyToSet = null;
            }
        }
        return outOptions;
    }

    public static class CommandLineArgs {
        public final List<String> args;
        public final Map<String, String> options;

        public CommandLineArgs(String[] args) {
            ArrayList<String> outArgs = new ArrayList<>(args.length);
            for (String arg : args) {
                if (arg.endsWith("\"") && arg.startsWith("\"")) {
                    arg = arg.substring(1, arg.length() - 1);
                }
                outArgs.add(arg);
            }
            this.args = Collections.unmodifiableList(outArgs);
            this.options = Collections.unmodifiableMap(extractOptions(outArgs));
        }

        public String getOption(String optionName) {
            return this.options.get(optionName);
        }

        @Override
        public String toString() {
            return "CommandLineArgs{" +
                    "args=" + args +
                    ", options=" + options +
                    '}';
        }
    }

    public static CommandLineArgs getCommandLineArgs(int pid) throws Exception {
        String cmdLineStr = getCommandLineStringFromPid(pid);
        return getCommandLineArgs(cmdLineStr);
    }

    public static CommandLineArgs getCommandLineArgs(String cmdLineStr) {
        CommandLine commandLine = CommandLine.parse(cmdLineStr);
        return new CommandLineArgs(commandLine.getArguments());
    }

}