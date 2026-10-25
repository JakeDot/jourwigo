package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;

public class AudioMedia extends Media {

    public static final JavaFunction PLAY = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof AudioMedia) {
                AudioMedia media = (AudioMedia) callFrame.get(0);
                media.play();
            }
            return 0;
        }
    };

    public static final JavaFunction STOP = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof AudioMedia) {
                AudioMedia media = (AudioMedia) callFrame.get(0);
                media.stop();
            }
            return 0;
        }
    };

    public static void register() {
        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(PLAY);
            Engine.instance.savegame.addJavafunc(STOP);
        }
    }

    public AudioMedia() {
        super();
        initTable();
    }

    public AudioMedia(String filePath, String type) {
        super(filePath, type);
        initTable();
    }

    private void initTable() {
        table.rawset("Play", PLAY);
        table.rawset("play", PLAY);
        table.rawset("Stop", STOP);
        table.rawset("stop", STOP);
    }

    public void stop() {
        Engine.ui.command("StopSound");
    }

    @Override
    protected String luaTostring() {
        return "an AudioMedia instance";
    }
}
