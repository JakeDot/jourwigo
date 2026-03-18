package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the Urwigo Wherigo player.
 *
 * <p>Usage:
 * <pre>
 *   java -jar urwigo.jar &lt;cartridge.gwc&gt; [latitude] [longitude] [altitude]
 * </pre>
 *
 * <p>The cartridge file must be a valid Wherigo GWC file. Latitude and longitude
 * are in decimal degrees (default: 0.0). Altitude is in metres (default: 0.0).
 *
 * <p>A save file will be created alongside the cartridge with a {@code .wgs} extension.
 */
public class WherigoPlayer {

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "--gui".equalsIgnoreCase(args[0])) {
            UrwigoDesktopGui.launch();
            return;
        }

        if ("--help".equalsIgnoreCase(args[0]) || "-h".equalsIgnoreCase(args[0])) {
            System.err.println("Usage: urwigo <cartridge.gwc> [latitude] [longitude] [altitude]");
            System.err.println();
            System.err.println("  --gui            Launch desktop GUI");
            System.err.println("  cartridge.gwc   Path to a Wherigo GWC cartridge file");
            System.err.println("  latitude        Starting latitude in decimal degrees (default: 0.0)");
            System.err.println("  longitude       Starting longitude in decimal degrees (default: 0.0)");
            System.err.println("  altitude        Starting altitude in metres (default: 0.0)");
            System.exit(1);
        }

        File gwcFile = new File(args[0]);
        if (!gwcFile.exists()) {
            System.err.println("Cartridge file not found: " + gwcFile.getAbsolutePath());
            System.exit(1);
        }

        double lat = args.length > 1 ? Double.parseDouble(args[1]) : 0.0;
        double lon = args.length > 2 ? Double.parseDouble(args[2]) : 0.0;
        double alt = args.length > 3 ? Double.parseDouble(args[3]) : 0.0;

        System.out.println("Urwigo - Wherigo Player");
        System.out.println("=======================");
        System.out.println("Cartridge : " + gwcFile.getName());
        System.out.printf("Start pos : lat=%.6f  lon=%.6f  alt=%.1fm%n", lat, lon, alt);
        System.out.println();

        // Build save-file path next to the cartridge
        String saveFileName = gwcFile.getName().replaceAll("(?i)\\.gwc$", "") + ".wgs";
        File saveFile = new File(gwcFile.getParentFile(), saveFileName);

        JavaSeekableFile seekable = new JavaSeekableFile(gwcFile);
        JavaFileHandle   savefh   = new JavaFileHandle(saveFile);

        CartridgeFile cf;
        try {
            cf = CartridgeFile.read(seekable, savefh);
        } catch (IOException e) {
            System.err.println("Failed to open cartridge: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Cartridge name    : " + cf.name);
        System.out.println("Cartridge type    : " + cf.type);
        System.out.println("Author            : " + cf.author);
        System.out.println("Version           : " + cf.version);
        System.out.println("Description       : " + cf.description);
        System.out.println();

        CountDownLatch         done = new CountDownLatch(1);
        ConsoleUI              ui  = new ConsoleUI() {
            @Override public void end() { super.end(); done.countDown(); }
        };
        ConsoleLocationService gps = new ConsoleLocationService(lat, lon, alt);

        Engine engine = Engine.newInstance(cf, null, ui, gps);

        // Set required WherigoLib.env values
        cgeo.geocaching.wherigo.openwig.WherigoLib.env.put(
            cgeo.geocaching.wherigo.openwig.WherigoLib.DEVICE_ID, "urwigo-console");
        cgeo.geocaching.wherigo.openwig.WherigoLib.env.put(
            cgeo.geocaching.wherigo.openwig.WherigoLib.PLATFORM, "Java/urwigo");

        // Start or restore game
        if (savefh.exists()) {
            System.out.println("Save file found - restoring game.");
            engine.restore();
        } else {
            System.out.println("Starting new game.");
            engine.start();
        }

        // Block the main thread until the engine signals game end
        try {
            done.await();
        } catch (InterruptedException e) {
            Engine.kill();
        }
    }
}
