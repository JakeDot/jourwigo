package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the jourwigo Wherigo player.
 *
 * <p>Usage:
 * <pre>
 *   java -jar jourwigo.jar &lt;cartridge.gwc&gt; [latitude] [longitude] [altitude]
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
            UrwigoDesktopGui.launchGui(args);
            return;
        }

        if ("--web".equalsIgnoreCase(args[0])) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            UrwigoWebServer.startAndBlock(port);
            return;
        }

        if ("--help".equalsIgnoreCase(args[0]) || "-h".equalsIgnoreCase(args[0])) {
            System.err.println("Usage: jourwigo <cartridge.gwc> [latitude] [longitude] [altitude]");
            System.err.println();
            System.err.println("  --gui            Launch desktop GUI");
            System.err.println("  --web [port]     Launch integrated web editor/player server (default port: 8080)");
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

        System.out.println("jourwigo - Wherigo Player");
        System.out.println("=======================");
        System.out.println("Cartridge : " + gwcFile.getName());
        System.out.printf("Start pos : lat=%.6f  lon=%.6f  alt=%.1fm%n", lat, lon, alt);
        System.out.println();

        JavaFileHandle savefh = IntegratedPlayerService.buildSaveHandle(gwcFile);

        CartridgeFile cf;
        try {
            cf = IntegratedPlayerService.readCartridge(gwcFile, savefh);
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

        Engine engine = IntegratedPlayerService.createEngine(cf, ui, gps, "jourwigo-console", "Java/jourwigo");

        // Start or restore game
        IntegratedPlayerService.startOrRestore(engine, savefh, System.out::println);

        // Block the main thread until the engine signals game end
        try {
            done.await();
        } catch (InterruptedException e) {
            Engine.kill();
        }
    }
}
