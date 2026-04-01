package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class UrwigoDesktopGui extends Application {

    private static String[] launchArgs = new String[0];

    private final TextField cartridgePathField = new TextField();
    private final TextField runLatitudeField = new TextField("0.0");
    private final TextField runLongitudeField = new TextField("0.0");
    private final TextField runAltitudeField = new TextField("0.0");
    private final TextArea outputArea = new TextArea();
    private final Label runnerStatus = new Label("Ready");
    private final Button runButton = new Button("Run");
    private final Button updateLocationButton = new Button("Update location");

    private final TextField createNameField = new TextField("New Cartridge");
    private final TextField createAuthorField = new TextField("Author");
    private final TextField createVersionField = new TextField("1.0");
    private final TextArea createDescriptionField = new TextArea("Cartridge description");
    private final TextField createLatitudeField = new TextField("0.0");
    private final TextField createLongitudeField = new TextField("0.0");
    private final TextField createAltitudeField = new TextField("0.0");
    private final TextField webPortField = new TextField("8080");
    private final TextField webUrlField = new TextField("http://localhost:8080/");
    private final WebView webView = new WebView();

    private DesktopLocationService runningLocationService;
    private boolean running;
    private Stage primaryStage;
    private HttpServer embeddedWebServer;
    private int embeddedWebPort = 8080;

    public static void launchGui(String[] args) {
        launchArgs = args == null ? new String[0] : args;
        Application.launch(UrwigoDesktopGui.class, launchArgs);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("jourwigo (cgeo/cgeo Wherigo runtime)");

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Run Cartridge", createRunnerTab()));
        tabs.getTabs().add(new Tab("Create Cartridge", createCreatorTab()));
        tabs.getTabs().add(new Tab("Web Integration", createWebIntegrationTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        Scene scene = new Scene(tabs, 980, 700);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createRunnerTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        HBox cartridgeRow = new HBox(8);
        Label cartridgeLabel = new Label("Cartridge (.gwc):");
        Button browse = new Button("Browse");
        browse.setOnAction(e -> chooseCartridge());
        HBox.setHgrow(cartridgePathField, Priority.ALWAYS);
        cartridgeRow.getChildren().addAll(cartridgeLabel, cartridgePathField, browse);

        GridPane locationGrid = new GridPane();
        locationGrid.setHgap(8);
        locationGrid.setVgap(6);
        locationGrid.add(new Label("Latitude"), 0, 0);
        locationGrid.add(new Label("Longitude"), 1, 0);
        locationGrid.add(new Label("Altitude"), 2, 0);
        locationGrid.add(runLatitudeField, 0, 1);
        locationGrid.add(runLongitudeField, 1, 1);
        locationGrid.add(runAltitudeField, 2, 1);
        locationGrid.add(updateLocationButton, 3, 1);
        updateLocationButton.setOnAction(e -> updateRunningLocation());
        updateLocationButton.setDisable(true);

        runButton.setOnAction(e -> startRun());
        HBox actionRow = new HBox(8, runButton);

        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        root.getChildren().addAll(cartridgeRow, locationGrid, actionRow, outputArea, runnerStatus);
        return root;
    }

    private VBox createCreatorTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.add(new Label("Name"), 0, 0);
        form.add(createNameField, 1, 0);
        form.add(new Label("Author"), 0, 1);
        form.add(createAuthorField, 1, 1);
        form.add(new Label("Version"), 0, 2);
        form.add(createVersionField, 1, 2);
        form.add(new Label("Start latitude"), 0, 3);
        form.add(createLatitudeField, 1, 3);
        form.add(new Label("Start longitude"), 0, 4);
        form.add(createLongitudeField, 1, 4);
        form.add(new Label("Start altitude"), 0, 5);
        form.add(createAltitudeField, 1, 5);

        createDescriptionField.setWrapText(true);
        createDescriptionField.setPrefRowCount(8);

        Button createButton = new Button("Create Lua cartridge template");
        createButton.setOnAction(e -> createTemplate());

        root.getChildren().addAll(form, new Label("Description"), createDescriptionField, createButton);
        return root;
    }

    private VBox createWebIntegrationTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        Label intro = new Label("Host the integrated web editor inside JavaFX (WebView).");
        HBox controls = new HBox(8);
        Label portLabel = new Label("Port");
        Button startButton = new Button("Start/Restart");
        Button loadButton = new Button("Load in WebView");
        HBox.setHgrow(webUrlField, Priority.ALWAYS);
        controls.getChildren().addAll(portLabel, webPortField, startButton, webUrlField, loadButton);

        startButton.setOnAction(e -> {
            if (ensureWebServerRunning()) {
                webView.getEngine().load(webUrlField.getText().trim());
            }
        });
        loadButton.setOnAction(e -> webView.getEngine().load(webUrlField.getText().trim()));

        VBox.setVgrow(webView, Priority.ALWAYS);
        root.getChildren().addAll(intro, controls, webView);
        return root;
    }

    private void chooseCartridge() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Wherigo Cartridge");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wherigo Cartridge (*.gwc)", "*.gwc"));
        File selected = chooser.showOpenDialog(primaryStage);
        if (selected != null) {
            cartridgePathField.setText(selected.getAbsolutePath());
        }
    }

    private void startRun() {
        if (running) {
            return;
        }
        File gwcFile = new File(cartridgePathField.getText().trim());
        if (!gwcFile.exists()) {
            showError("Cartridge file not found.");
            return;
        }

        double lat = parseDouble(runLatitudeField.getText(), 0.0);
        double lon = parseDouble(runLongitudeField.getText(), 0.0);
        double alt = parseDouble(runAltitudeField.getText(), 0.0);

        JavaFileHandle savefh = IntegratedPlayerService.buildSaveHandle(gwcFile);
        final CartridgeFile cartridgeFile;
        try {
            cartridgeFile = IntegratedPlayerService.readCartridge(gwcFile, savefh);
        } catch (IOException e) {
            showError("Failed to open cartridge: " + e.getMessage());
            return;
        }

        running = true;
        runButton.setDisable(true);
        updateLocationButton.setDisable(false);
        outputArea.clear();
        appendOutput("Integrated cgeo/cgeo player started");
        appendOutput("Cartridge: " + cartridgeFile.name);
        appendOutput("Author: " + cartridgeFile.author);
        runnerStatus.setText("Running");

        runningLocationService = new DesktopLocationService(lat, lon, alt);
        DesktopUI ui = new DesktopUI(this::appendOutput, this::setRunnerStatus, this::onRunEnded);
        Engine engine;
        try {
            engine = IntegratedPlayerService.createEngine(
                cartridgeFile, ui, runningLocationService, "jourwigo-javafx", "JavaFX/cgeo-cgeo");
        } catch (IOException e) {
            showError("Unable to create engine: " + e.getMessage());
            onRunEnded();
            return;
        }

        try {
            IntegratedPlayerService.startOrRestore(engine, savefh, this::appendOutput);
        } catch (IOException e) {
            showError("Unable to check save file: " + e.getMessage());
            onRunEnded();
        }
    }

    private void updateRunningLocation() {
        if (runningLocationService == null) {
            return;
        }
        double lat = parseDouble(runLatitudeField.getText(), runningLocationService.getLatitude());
        double lon = parseDouble(runLongitudeField.getText(), runningLocationService.getLongitude());
        double alt = parseDouble(runAltitudeField.getText(), runningLocationService.getAltitude());
        runningLocationService.setPosition(lat, lon, alt);
        appendOutput("Location updated to lat=" + lat + ", lon=" + lon + ", alt=" + alt);
    }

    private void createTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Lua cartridge template");
        chooser.setInitialFileName("cartridge.lua");
        File selected = chooser.showSaveDialog(primaryStage);
        if (selected == null) {
            return;
        }
        Path output = selected.toPath();
        try {
            CartridgeTemplateWriter.writeLuaTemplate(
                output,
                createNameField.getText(),
                createAuthorField.getText(),
                createVersionField.getText(),
                createDescriptionField.getText(),
                parseDouble(createLatitudeField.getText(), 0.0),
                parseDouble(createLongitudeField.getText(), 0.0),
                parseDouble(createAltitudeField.getText(), 0.0)
            );
            showInfo("Template created:\n" + output
                + "\n\nCompile this Lua script with jourwigo-compatible tooling to produce a .gwc cartridge.");
        } catch (IOException e) {
            showError("Failed to write template: " + e.getMessage());
        }
    }

    private void onRunEnded() {
        Platform.runLater(() -> {
            running = false;
            runButton.setDisable(false);
            updateLocationButton.setDisable(true);
            runnerStatus.setText("Ready");
        });
    }

    private void appendOutput(String line) {
        Platform.runLater(() -> outputArea.appendText(line + System.lineSeparator()));
    }

    private void setRunnerStatus(String status) {
        Platform.runLater(() -> runnerStatus.setText(status));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("jourwigo");
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText("jourwigo");
        alert.showAndWait();
    }

    static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static int parseWebPort(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 && parsed <= 65535 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean ensureWebServerRunning() {
        int targetPort = parseWebPort(webPortField.getText(), 8080);
        if (embeddedWebServer != null && embeddedWebPort == targetPort) {
            webUrlField.setText("http://localhost:" + embeddedWebPort + "/");
            return true;
        }
        if (embeddedWebServer != null) {
            embeddedWebServer.stop(0);
            embeddedWebServer = null;
        }
        try {
            embeddedWebServer = UrwigoWebServer.start(targetPort);
            embeddedWebPort = targetPort;
            webUrlField.setText("http://localhost:" + embeddedWebPort + "/");
            showInfo("Integrated web editor started on port " + embeddedWebPort);
            return true;
        } catch (IOException e) {
            showError("Failed to start integrated web editor: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void stop() {
        if (embeddedWebServer != null) {
            embeddedWebServer.stop(0);
            embeddedWebServer = null;
        }
    }

    private static final class DesktopLocationService implements LocationService {
        private volatile double latitude;
        private volatile double longitude;
        private volatile double altitude;
        private volatile double heading;
        private volatile double precision;
        private volatile int state;

        private DesktopLocationService(double latitude, double longitude, double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.heading = 0.0;
            this.precision = 5.0; // precision in meters, matching LocationService contract
            this.state = ONLINE;
        }

        void setPosition(double latitude, double longitude, double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }

        @Override
        public double getLatitude() {
            return latitude;
        }

        @Override
        public double getLongitude() {
            return longitude;
        }

        @Override
        public double getAltitude() {
            return altitude;
        }

        @Override
        public double getHeading() {
            return heading;
        }

        @Override
        public double getPrecision() {
            return precision;
        }

        @Override
        public int getState() {
            return state;
        }

        @Override
        public void connect() {
            state = ONLINE;
        }

        @Override
        public void disconnect() {
            state = OFFLINE;
        }
    }

    private static final class DesktopUI implements UI {
        private static final Double[] LUA_INDEX_CACHE = createLuaIndexCache(256);

        private final java.util.function.Consumer<String> append;
        private final java.util.function.Consumer<String> status;
        private final Runnable onEnd;

        private DesktopUI(java.util.function.Consumer<String> append, java.util.function.Consumer<String> status,
                          Runnable onEnd) {
            this.append = append;
            this.status = status;
            this.onEnd = onEnd;
        }

        @Override
        public void refresh() {
            // no explicit refresh needed
        }

        @Override
        public void start() {
            append.accept("[Wherigo] Game started.");
            status.accept("Game running");
        }

        @Override
        public void end() {
            append.accept("[Wherigo] Game ended.");
            status.accept("Game ended");
            onEnd.run();
        }

        @Override
        public void showError(String msg) {
            append.accept("[ERROR] " + msg);
            callOnFx(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, msg);
                alert.setHeaderText("jourwigo");
                alert.showAndWait();
                return null;
            });
        }

        @Override
        public void debugMsg(String msg) {
            append.accept("[DEBUG] " + msg);
        }

        @Override
        public void setStatusText(String text) {
            if (text != null && !text.isEmpty()) {
                status.accept(text);
            }
        }

        @Override
        public void pushDialog(String[] texts, Media[] media, String button1, String button2, LuaClosure callback) {
            String primary = button1 != null ? button1 : "OK";
            String secondary = button2;
            String message = texts == null || texts.length == 0 ? "(no message)" : String.join("\n\n", texts);
            String callbackValue;
            if (secondary == null) {
                callOnFx(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
                    alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK);
                    alert.setHeaderText("Dialog");
                    alert.showAndWait();
                    return null;
                });
                callbackValue = "Button1";
            } else {
                callbackValue = callOnFx(() -> {
                    javafx.scene.control.ButtonType primaryButton = new javafx.scene.control.ButtonType(primary);
                    javafx.scene.control.ButtonType secondaryButton = new javafx.scene.control.ButtonType(secondary);
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, primaryButton, secondaryButton);
                    alert.setHeaderText("Dialog");
                    javafx.scene.control.ButtonType result = alert.showAndWait().orElse(primaryButton);
                    return result == secondaryButton ? "Button2" : "Button1";
                });
            }
            invokeCallbackIfPresent(callback, callbackValue);
        }

        @Override
        public void pushInput(EventTable input) {
            String text = (String) input.table.rawget("Text");
            String type = (String) input.table.rawget("Type");
            String response;

            if ("MultipleChoice".equalsIgnoreCase(type)) {
                Object[] options = toOptions(input.table.rawget("Choices"));
                response = callOnFx(() -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>(
                        options.length > 0 ? options[0].toString() : "",
                        java.util.Arrays.stream(options).map(Object::toString).toList()
                    );
                    dialog.setHeaderText(text != null ? text : "Choose an option");
                    return dialog.showAndWait().orElse(null);
                });
            } else {
                response = callOnFx(() -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setHeaderText(text != null ? text : "Input");
                    return dialog.showAndWait().orElse(null);
                });
            }
            Engine.callEvent(input, "OnGetInput", response);
        }

        @Override
        public void showScreen(ScreenId screenId, EventTable details) {
            StringBuilder line = new StringBuilder("=== Screen: " + screenName(screenId) + " ===");
            if (details != null && details.name != null) {
                line.append(" ").append(details.name);
            }
            append.accept(line.toString());
            if (details != null && details.description != null && !details.description.isEmpty()) {
                append.accept(details.description);
            }
        }

        @Override
        public void playSound(byte[] data, String mime) {
            append.accept("[Sound] " + mime + " (" + (data != null ? data.length : 0) + " bytes)");
        }

        @Override
        public void blockForSaving() {
            status.accept("Saving...");
        }

        @Override
        public void unblock() {
            status.accept("Game running");
        }

        @Override
        public void command(String cmd) {
            append.accept("[Command] " + cmd);
        }

        private static Object[] toOptions(Object rawChoices) {
            if (rawChoices instanceof LuaTable table) {
                int count = table.len();
                Object[] options = new Object[count];
                for (int i = 1; i <= count; i++) {
                    Object option = table.rawget(luaIndex(i));
                    options[i - 1] = option != null ? option.toString() : "";
                }
                return options;
            }
            return new Object[0];
        }

        private static Double luaIndex(int index) {
            return index < LUA_INDEX_CACHE.length ? LUA_INDEX_CACHE[index] : Double.valueOf(index);
        }

        private static Double[] createLuaIndexCache(int size) {
            Double[] cache = new Double[size + 1];
            for (int i = 0; i < cache.length; i++) {
                cache[i] = Double.valueOf(i);
            }
            return cache;
        }

        private static String screenName(ScreenId screenId) {
            return switch (screenId) {
                case MAINSCREEN -> "Main";
                case DETAILSCREEN -> "Detail";
                case INVENTORYSCREEN -> "Inventory";
                case ITEMSCREEN -> "Item";
                case LOCATIONSCREEN -> "Locations";
                case TASKSCREEN -> "Tasks";
                case UNKNOWN -> "Unknown";
            };
        }

        private static void invokeCallbackIfPresent(LuaClosure callback, String value) {
            if (callback != null) {
                Engine.invokeCallback(callback, value);
            }
        }

        private static <T> T callOnFx(Callable<T> callable) {
            if (Platform.isFxApplicationThread()) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to run JavaFX action", e);
                }
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<T> result = new AtomicReference<>();
            AtomicReference<RuntimeException> error = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    result.set(callable.call());
                } catch (Exception e) {
                    error.set(new IllegalStateException("Failed to run JavaFX action", e));
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for JavaFX action", e);
            }
            if (error.get() != null) {
                throw error.get();
            }
            return result.get();
        }
    }
}
