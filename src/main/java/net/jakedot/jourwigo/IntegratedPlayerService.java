package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.WherigoLib;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Shared integration layer around the cgeo/cgeo runtime used by CLI, desktop and web entry points.
 */
public final class IntegratedPlayerService {

    private IntegratedPlayerService() {
        // utility class
    }

    public static JavaFileHandle buildSaveHandle(File cartridgeFile) {
        String saveFileName = cartridgeFile.getName().replaceAll("(?i)\\.gwc$", "") + ".wgs";
        File saveFile = new File(cartridgeFile.getParentFile(), saveFileName);
        return new JavaFileHandle(saveFile);
    }

    public static CartridgeFile readCartridge(File cartridgeFile, JavaFileHandle saveHandle) throws IOException {
        return CartridgeFile.read(new JavaSeekableFile(cartridgeFile), saveHandle);
    }

    public static Engine createEngine(CartridgeFile cartridgeFile, UI ui, LocationService locationService,
                                      String deviceId, String platform) throws IOException {
        Engine engine = Engine.newInstance(cartridgeFile, null, ui, locationService);
        WherigoLib.env.put(WherigoLib.DEVICE_ID, deviceId);
        WherigoLib.env.put(WherigoLib.PLATFORM, platform);
        return engine;
    }

    public static void startOrRestore(Engine engine, JavaFileHandle saveHandle, Consumer<String> log) throws IOException {
        if (saveHandle.exists()) {
            log.accept("Save file found - restoring game.");
            engine.restore();
        } else {
            log.accept("Starting new game.");
            engine.start();
        }
    }
}
