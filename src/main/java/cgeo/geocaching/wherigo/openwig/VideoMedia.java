package cgeo.geocaching.wherigo.openwig;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;

public class VideoMedia extends Media {

    public static final JavaFunction PLAY = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof VideoMedia) {
                VideoMedia media = (VideoMedia) callFrame.get(0);
                media.play();
            }
            return 0;
        }
    };

    public static final JavaFunction SHOW = new JavaFunction() {
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (nArguments >= 1 && callFrame.get(0) instanceof VideoMedia) {
                VideoMedia media = (VideoMedia) callFrame.get(0);
                media.show();
            }
            return 0;
        }
    };

    public static void register() {
        if (Engine.instance != null && Engine.instance.savegame != null) {
            Engine.instance.savegame.addJavafunc(PLAY);
            Engine.instance.savegame.addJavafunc(SHOW);
        }
    }

    public VideoMedia() {
        super();
        initTable();
    }

    public VideoMedia(String filePath, String type) {
        super(filePath, type);
        initTable();
    }

    private void initTable() {
        table.rawset("Play", PLAY);
        table.rawset("play", PLAY);
        table.rawset("Show", SHOW);
        table.rawset("show", SHOW);
    }

    public void show() {
        Engine.dialog(new String[]{name != null ? name : (altText != null ? altText : "Video")}, new Media[]{this});
    }

    @Override
    protected String luaTostring() {
        return "a VideoMedia instance";
    }
}
