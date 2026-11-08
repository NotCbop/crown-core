package com.github.NGoedix.videoplayer.anticheat;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers the CrownChampionshipUtils plugin's {@code acscan;<probe|probe...>} with what this game
 * instance has loaded. It only gathers facts; deciding what counts as a cheat is the plugin's job
 * (its config holds the signatures, so they can change without a mod update).
 * <p>
 * Stays inside the Minecraft process: no process list, no reading of files on disk, and native
 * libraries and agents are reported by file name only, never by path. Nothing is sent unless a
 * server asks. Reply bodies: {@code acreport;<section>;<a|b|c>}, ending with {@code acreport;done}.
 */
@Environment(EnvType.CLIENT)
public final class AntiCheatScanner {

    /** Servers allowed to request a scan (lower-case host, no port). Empty = any server that asks. */
    private static final Set<String> ALLOWED_SERVER_HOSTS = Set.of("play.crown-championship.cc");

    private static final int MAX_PROBES = 200;
    /** Vanilla caps a serverbound custom payload at 32767 bytes. */
    private static final int MAX_BODY_BYTES = 30000;

    /** Plain vanilla key names look like key.attack; hotbar slots are the one dotted vanilla family. */
    private static final Pattern VANILLA_KEY = Pattern.compile("key\\.([A-Za-z]+|hotbar\\.\\d+)");
    /** The path at the end of a VM.dynlibs line (Windows drive path or absolute POSIX path). */
    private static final Pattern LIB_PATH = Pattern.compile("([A-Za-z]:\\\\.*|/.*)$");

    private AntiCheatScanner() {}

    /** Must run on the client thread (reads the key mappings); the slower checks go to a worker. */
    public static void run(String probeList) {
        Minecraft mc = Minecraft.getInstance();
        if (!serverAllowed(mc)) return;

        List<String> keys = modKeys(mc);
        Thread worker = new Thread(() -> {
            List<String> mods = modList();
            List<String> found = probe(probeList);
            List<String> agents = jvmAgents();
            List<String> libs = nativeLibraries();
            mc.execute(() -> {
                send("mods", mods);
                send("keys", keys);
                send("found", found);
                send("agents", agents);
                send("libs", libs);
                PacketHandler.sendC2SPlayBody("acreport;done");
            });
        }, "Crown anticheat scan");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean serverAllowed(Minecraft mc) {
        if (mc.isLocalServer()) return false;
        if (ALLOWED_SERVER_HOSTS.isEmpty()) return true;
        ServerData server = mc.getCurrentServer();
        if (server == null) return false;
        String host = server.ip.toLowerCase(Locale.ROOT);
        int port = host.lastIndexOf(':');
        return ALLOWED_SERVER_HOSTS.contains(port < 0 ? host : host.substring(0, port));
    }

    private static List<String> modList() {
        List<String> out = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            out.add(mod.getMetadata().getId() + "@" + mod.getMetadata().getVersion().getFriendlyString());
        }
        return out;
    }

    /** Keys registered by mods. Clients with a renamed mod id usually keep their key names. */
    private static List<String> modKeys(Minecraft mc) {
        List<String> out = new ArrayList<>();
        for (KeyMapping key : mc.options.keyMappings) {
            if (!VANILLA_KEY.matcher(key.getName()).matches()) out.add(key.getName());
        }
        return out;
    }

    /**
     * Which of the server's probe names exist on the game's classpath. Looks resources up without
     * loading them, and only accepts mixin configs and class files so a server can't poke at
     * anything else.
     */
    private static List<String> probe(String probeList) {
        List<String> found = new ArrayList<>();
        if (probeList.isEmpty()) return found;
        ClassLoader loader = AntiCheatScanner.class.getClassLoader();
        String[] probes = probeList.split("\\|");
        for (int i = 0; i < probes.length && i < MAX_PROBES; i++) {
            String name = probes[i].trim();
            boolean allowed = name.endsWith(".json") || name.endsWith(".class");
            if (!allowed || name.contains("..") || name.startsWith("/")) continue;
            if (loader.getResource(name) != null) found.add(name);
        }
        return found;
    }

    /** File names of agents given on the java command line. */
    private static List<String> jvmAgents() {
        List<String> out = new ArrayList<>();
        try {
            for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (!arg.startsWith("-javaagent:") && !arg.startsWith("-agentpath:") && !arg.startsWith("-agentlib:")) continue;
                String target = arg.substring(arg.indexOf(':') + 1);
                int options = target.indexOf('=');
                if (options >= 0) target = target.substring(0, options);
                out.add(fileName(target));
            }
        } catch (Exception e) {
            Reference.LOGGER.debug("Could not read JVM arguments", e);
        }
        return out;
    }

    /** File names of native libraries in this process, minus the OS's and the JVM's own. */
    private static List<String> nativeLibraries() {
        Set<String> out = new LinkedHashSet<>();
        try {
            Object result = ManagementFactory.getPlatformMBeanServer().invoke(
                    new ObjectName("com.sun.management:type=DiagnosticCommand"), "vmDynlibs",
                    new Object[]{null}, new String[]{"[Ljava.lang.String;"});
            String javaHome = System.getProperty("java.home", "").toLowerCase(Locale.ROOT);
            String windows = System.getenv("SystemRoot");
            String windowsDir = windows == null ? null : windows.toLowerCase(Locale.ROOT);
            for (String line : result.toString().split("\\R")) {
                Matcher m = LIB_PATH.matcher(line.trim());
                if (!m.find()) continue;
                String path = m.group(1).trim();
                String lower = path.toLowerCase(Locale.ROOT);
                boolean system = (!javaHome.isEmpty() && lower.startsWith(javaHome))
                        || (windowsDir != null && lower.startsWith(windowsDir))
                        || lower.startsWith("/usr/") || lower.startsWith("/lib") || lower.startsWith("/system/");
                if (!system) out.add(fileName(path));
            }
        } catch (Exception e) {
            // Some runtimes leave out the diagnostic command; the other checks still report.
            Reference.LOGGER.debug("Could not list native libraries", e);
        }
        return new ArrayList<>(out);
    }

    private static String fileName(String path) {
        int cut = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return cut < 0 ? path : path.substring(cut + 1);
    }

    private static void send(String section, List<String> items) {
        StringBuilder body = new StringBuilder("acreport;").append(section).append(';');
        int bytes = body.length();
        boolean first = true;
        for (String item : items) {
            String clean = item.replace('|', '_').replace(';', '_');
            bytes += clean.getBytes(StandardCharsets.UTF_8).length + 1;
            if (bytes > MAX_BODY_BYTES) break;
            if (!first) body.append('|');
            body.append(clean);
            first = false;
        }
        PacketHandler.sendC2SPlayBody(body.toString());
    }
}
