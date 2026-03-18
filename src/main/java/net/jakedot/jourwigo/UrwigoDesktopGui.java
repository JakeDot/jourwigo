package net.jakedot.jourwigo;

import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.WherigoLib;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.LocationService;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class UrwigoDesktopGui {

    private final JFrame frame = new JFrame("jourwigo");
    private final JTextField cartridgePathField = new JTextField();
    private final JTextField runLatitudeField = new JTextField("0.0");
    private final JTextField runLongitudeField = new JTextField("0.0");
    private final JTextField runAltitudeField = new JTextField("0.0");
    private final JButton runButton = new JButton("Run");
    private final JButton applyLocationButton = new JButton("Update location");
    private final JTextArea outputArea = new JTextArea();
    private final JLabel runnerStatus = new JLabel("Ready");

    private final JTextField createNameField = new JTextField("New Cartridge");
    private final JTextField createAuthorField = new JTextField("Author");
    private final JTextField createVersionField = new JTextField("1.0");
    private final JTextArea createDescriptionArea = new JTextArea("Cartridge description");
    private final JTextField createLatitudeField = new JTextField("0.0");
    private final JTextField createLongitudeField = new JTextField("0.0");
    private final JTextField createAltitudeField = new JTextField("0.0");

    private DesktopLocationService runningLocationService;
    private boolean running;

    public static void launch() {
        SwingUtilities.invokeLater(() -> new UrwigoDesktopGui().initAndShow());
    }

    private void initAndShow() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));
        frame.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Run Cartridge", createRunnerTab());
        tabs.add("Create Cartridge", createCreatorTab());
        frame.add(tabs, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createRunnerTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        JPanel filePanel = new JPanel(new BorderLayout(6, 6));
        filePanel.add(new JLabel("Cartridge (.gwc):"), BorderLayout.WEST);
        filePanel.add(cartridgePathField, BorderLayout.CENTER);
        JButton browse = new JButton("Browse");
        browse.addActionListener(e -> chooseCartridge());
        filePanel.add(browse, BorderLayout.EAST);
        top.add(filePanel, BorderLayout.NORTH);

        JPanel locationPanel = new JPanel(new GridLayout(2, 4, 6, 6));
        locationPanel.add(new JLabel("Latitude"));
        locationPanel.add(new JLabel("Longitude"));
        locationPanel.add(new JLabel("Altitude"));
        locationPanel.add(new JLabel(""));
        locationPanel.add(runLatitudeField);
        locationPanel.add(runLongitudeField);
        locationPanel.add(runAltitudeField);
        locationPanel.add(applyLocationButton);
        top.add(locationPanel, BorderLayout.CENTER);
        applyLocationButton.addActionListener(e -> updateRunningLocation());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runButton.addActionListener(e -> startRun());
        actions.add(runButton);
        top.add(actions, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        panel.add(runnerStatus, BorderLayout.SOUTH);

        applyLocationButton.setEnabled(false);
        return panel;
    }

    private JPanel createCreatorTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Name"));
        form.add(createNameField);
        form.add(new JLabel("Author"));
        form.add(createAuthorField);
        form.add(new JLabel("Version"));
        form.add(createVersionField);
        form.add(new JLabel("Start latitude"));
        form.add(createLatitudeField);
        form.add(new JLabel("Start longitude"));
        form.add(createLongitudeField);
        form.add(new JLabel("Start altitude"));
        form.add(createAltitudeField);
        panel.add(form, BorderLayout.NORTH);

        createDescriptionArea.setLineWrap(true);
        createDescriptionArea.setWrapStyleWord(true);
        JPanel descriptionPanel = new JPanel(new BorderLayout(4, 4));
        descriptionPanel.add(new JLabel("Description"), BorderLayout.NORTH);
        descriptionPanel.add(new JScrollPane(createDescriptionArea), BorderLayout.CENTER);
        panel.add(descriptionPanel, BorderLayout.CENTER);

        JButton create = new JButton("Create Lua cartridge template");
        create.addActionListener(e -> createTemplate());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(create);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void chooseCartridge() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Wherigo Cartridge (*.gwc)", "gwc"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            cartridgePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startRun() {
        if (running) {
            return;
        }
        File gwcFile = new File(cartridgePathField.getText().trim());
        if (!gwcFile.exists()) {
            JOptionPane.showMessageDialog(frame, "Cartridge file not found.", "jourwigo", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double lat = parseDouble(runLatitudeField.getText(), 0.0);
        double lon = parseDouble(runLongitudeField.getText(), 0.0);
        double alt = parseDouble(runAltitudeField.getText(), 0.0);

        String saveFileName = gwcFile.getName().replaceAll("(?i)\\.gwc$", "") + ".wgs";
        File saveFile = new File(gwcFile.getParentFile(), saveFileName);
        JavaSeekableFile seekable = new JavaSeekableFile(gwcFile);
        JavaFileHandle savefh = new JavaFileHandle(saveFile);
        final CartridgeFile cartridgeFile;
        try {
            cartridgeFile = CartridgeFile.read(seekable, savefh);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to open cartridge: " + e.getMessage(), "jourwigo",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        running = true;
        runButton.setEnabled(false);
        applyLocationButton.setEnabled(true);
        outputArea.setText("");
        appendOutput("Urwigo-style runner started");
        appendOutput("Cartridge: " + cartridgeFile.name);
        appendOutput("Author: " + cartridgeFile.author);
        runnerStatus.setText("Running");

        runningLocationService = new DesktopLocationService(lat, lon, alt);
        DesktopUI ui = new DesktopUI(frame, outputArea, runnerStatus, this::onRunEnded);
        Engine engine = Engine.newInstance(cartridgeFile, null, ui, runningLocationService);
        WherigoLib.env.put(WherigoLib.DEVICE_ID, "jourwigo-desktop");
        WherigoLib.env.put(WherigoLib.PLATFORM, "Java/UrwigoDesktop");

        try {
            if (savefh.exists()) {
                appendOutput("Save file found - restoring game");
                engine.restore();
            } else {
                appendOutput("Starting new game");
                engine.start();
            }
        } catch (IOException e) {
            ui.showError("Unable to check save file: " + e.getMessage());
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
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Lua cartridge template");
        chooser.setSelectedFile(new File("cartridge.lua"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path output = chooser.getSelectedFile().toPath();
        try {
            CartridgeTemplateWriter.writeLuaTemplate(
                output,
                createNameField.getText(),
                createAuthorField.getText(),
                createVersionField.getText(),
                createDescriptionArea.getText(),
                parseDouble(createLatitudeField.getText(), 0.0),
                parseDouble(createLongitudeField.getText(), 0.0),
                parseDouble(createAltitudeField.getText(), 0.0)
            );
            JOptionPane.showMessageDialog(frame,
                "Template created:\n" + output + "\n\nCompile the Lua script with Urwigo or other Wherigo tooling to produce a .gwc cartridge.",
                "jourwigo",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to write template: " + e.getMessage(), "jourwigo",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRunEnded() {
        SwingUtilities.invokeLater(() -> {
            running = false;
            runButton.setEnabled(true);
            applyLocationButton.setEnabled(false);
            runnerStatus.setText("Ready");
        });
    }

    private void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> outputArea.append(text + System.lineSeparator()));
    }

    static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
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
            this.precision = 5.0;
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
        private final JFrame frame;
        private final JTextArea output;
        private final JLabel status;
        private final Runnable onEnd;

        private DesktopUI(JFrame frame, JTextArea output, JLabel status, Runnable onEnd) {
            this.frame = frame;
            this.output = output;
            this.status = status;
            this.onEnd = onEnd;
        }

        @Override
        public void refresh() {
            // output is append-only; no explicit refresh needed
        }

        @Override
        public void start() {
            append("[Wherigo] Game started.");
            SwingUtilities.invokeLater(() -> status.setText("Game running"));
        }

        @Override
        public void end() {
            append("[Wherigo] Game ended.");
            SwingUtilities.invokeLater(() -> status.setText("Game ended"));
            onEnd.run();
        }

        @Override
        public void showError(String msg) {
            append("[ERROR] " + msg);
            runOnEdt(() -> JOptionPane.showMessageDialog(frame, msg, "jourwigo", JOptionPane.ERROR_MESSAGE));
        }

        @Override
        public void debugMsg(String msg) {
            append("[DEBUG] " + msg);
        }

        @Override
        public void setStatusText(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            SwingUtilities.invokeLater(() -> status.setText(text));
        }

        @Override
        public void pushDialog(String[] texts, Media[] media, String button1, String button2, LuaClosure callback) {
            String primary = button1 != null ? button1 : "OK";
            String secondary = button2;
            String message = (texts == null || texts.length == 0)
                ? "(no message)"
                : String.join("\n\n", texts);
            String callbackValue;
            if (secondary == null) {
                runOnEdt(() -> JOptionPane.showMessageDialog(frame, message, "Dialog", JOptionPane.INFORMATION_MESSAGE));
                callbackValue = "Button1";
            } else {
                int choice = callOnEdt(() -> JOptionPane.showOptionDialog(
                    frame,
                    message,
                    "Dialog",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[] {primary, secondary},
                    primary
                ));
                callbackValue = choice == 1 ? "Button2" : "Button1";
            }
            invokeCallbackIfPresent(callback, callbackValue);
        }

        @Override
        public void pushInput(EventTable input) {
            String text = (String) input.table.rawget("Text");
            String type = (String) input.table.rawget("Type");
            String response;

            if ("MultipleChoice".equalsIgnoreCase(type)) {
                Object choices = input.table.rawget("Choices");
                Object[] options = toOptions(choices);
                Object selected = callOnEdt(() -> JOptionPane.showInputDialog(
                    frame,
                    text != null ? text : "Choose an option",
                    "Input",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options.length > 0 ? options[0] : null
                ));
                response = selected == null ? null : selected.toString();
            } else {
                response = callOnEdt(() -> JOptionPane.showInputDialog(frame, text != null ? text : "Input"));
            }
            Engine.callEvent(input, "OnGetInput", response);
        }

        @Override
        public void showScreen(ScreenId screenId, EventTable details) {
            StringBuilder line = new StringBuilder("=== Screen: " + screenName(screenId) + " ===");
            if (details != null && details.name != null) {
                line.append(" ").append(details.name);
            }
            append(line.toString());
            if (details != null && details.description != null && !details.description.isEmpty()) {
                append(details.description);
            }
        }

        @Override
        public void playSound(byte[] data, String mime) {
            append("[Sound] " + mime + " (" + (data != null ? data.length : 0) + " bytes)");
        }

        @Override
        public void blockForSaving() {
            SwingUtilities.invokeLater(() -> status.setText("Saving..."));
        }

        @Override
        public void unblock() {
            SwingUtilities.invokeLater(() -> status.setText("Game running"));
        }

        @Override
        public void command(String cmd) {
            append("[Command] " + cmd);
        }

        private void append(String line) {
            SwingUtilities.invokeLater(() -> output.append(line + System.lineSeparator()));
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

        private static Object[] toOptions(Object rawChoices) {
            if (rawChoices instanceof LuaTable table) {
                int count = table.len();
                Object[] options = new Object[count];
                for (int i = 1; i <= count; i++) {
                    Object option = table.rawget(Double.valueOf(i));
                    options[i - 1] = option != null ? option.toString() : "";
                }
                return options;
            }
            return new Object[0];
        }

        private static void invokeCallbackIfPresent(LuaClosure callback, String value) {
            if (callback != null) {
                Engine.invokeCallback(callback, value);
            }
        }

        private static void runOnEdt(Runnable runnable) {
            callOnEdt(() -> {
                runnable.run();
                return null;
            });
        }

        private static <T> T callOnEdt(Callable<T> callable) {
            if (SwingUtilities.isEventDispatchThread()) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to run UI action", e);
                }
            }

            final Object[] result = new Object[1];
            final RuntimeException[] error = new RuntimeException[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        result[0] = callable.call();
                    } catch (Exception e) {
                        error[0] = new IllegalStateException("Failed to run UI action", e);
                    }
                });
            } catch (Exception e) {
                throw new IllegalStateException("Failed to dispatch UI action", e);
            }
            if (error[0] != null) {
                throw error[0];
            }
            @SuppressWarnings("unchecked")
            T value = (T) result[0];
            return value;
        }
    }
}
