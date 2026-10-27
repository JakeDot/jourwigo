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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    private final Label objectsStatusLabel = new Label("Objects included: 0 objects");

    private final List<LuaObjectBuilder> cartridgeObjects = new ArrayList<>();

    private DesktopLocationService runningLocationService;
    private boolean running;
    private Stage primaryStage;

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
        tabs.getTabs().add(new Tab("Object Builder", createObjectBuilderTab()));
        tabs.getTabs().add(new Tab("Dialog Builder", createDialogBuilderTab()));
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
        createDescriptionField.setPrefRowCount(6);

        Button createLuaButton = new Button("Create Lua Cartridge Template (.lua)");
        createLuaButton.setOnAction(e -> createTemplate());

        Button compileGwcButton = new Button("Compile Cartridge (.gwc)");
        compileGwcButton.setOnAction(e -> compileGwcCartridge());

        Button clearObjectsBtn = new Button("Clear Included Objects");
        clearObjectsBtn.setOnAction(e -> {
            cartridgeObjects.clear();
            updateObjectsStatus();
        });

        HBox actionRow = new HBox(8, createLuaButton, compileGwcButton, clearObjectsBtn);

        root.getChildren().addAll(form, new Label("Description"), createDescriptionField, objectsStatusLabel, actionRow);
        return root;
    }

    private VBox createObjectBuilderTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        HBox templateRow = new HBox(8);
        Label templateLabel = new Label("Template:");
        ComboBox<LuaObjectBuilder.Template> templateBox = new ComboBox<>();
        templateBox.getItems().addAll(LuaObjectBuilder.Template.values());
        templateBox.setValue(LuaObjectBuilder.Template.ZONE);

        templateRow.getChildren().addAll(templateLabel, templateBox);

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(8);
        headerGrid.setVgap(8);

        TextField varNameField = new TextField();
        TextField constructorField = new TextField();

        headerGrid.add(new Label("Variable Name:"), 0, 0);
        headerGrid.add(varNameField, 1, 0);
        headerGrid.add(new Label("Constructor / Expr:"), 0, 1);
        headerGrid.add(constructorField, 1, 1);

        VBox propertiesBox = new VBox(6);
        ScrollPane propScrollPane = new ScrollPane(propertiesBox);
        propScrollPane.setFitToWidth(true);
        propScrollPane.setPrefHeight(180);

        Button addPropertyBtn = new Button("+ Add Property");
        HBox propHeader = new HBox(10, new Label("Properties:"), addPropertyBtn);

        TextArea luaPreviewArea = new TextArea();
        luaPreviewArea.setEditable(true);
        luaPreviewArea.setWrapText(true);
        VBox.setVgrow(luaPreviewArea, Priority.ALWAYS);

        Button generateBtn = new Button("Generate Lua Code");
        Button copyBtn = new Button("Copy to Clipboard");
        Button addToCartridgeBtn = new Button("Add Object to Cartridge");
        HBox actionRow = new HBox(8, generateBtn, copyBtn, addToCartridgeBtn);

        java.util.function.Supplier<LuaObjectBuilder> buildCurrentObject = () -> {
            LuaObjectBuilder builder = new LuaObjectBuilder(varNameField.getText(), constructorField.getText());
            for (javafx.scene.Node node : propertiesBox.getChildren()) {
                if (node instanceof HBox row) {
                    TextField keyField = (TextField) row.getChildren().get(0);
                    @SuppressWarnings("unchecked")
                    ComboBox<LuaObjectBuilder.PropertyType> typeBox = (ComboBox<LuaObjectBuilder.PropertyType>) row.getChildren().get(1);
                    TextField valField = (TextField) row.getChildren().get(2);
                    builder.addProperty(keyField.getText(), valField.getText(), typeBox.getValue());
                }
            }
            return builder;
        };

        Runnable generateLua = () -> {
            luaPreviewArea.setText(buildCurrentObject.get().toLuaCode());
        };

        Consumer<LuaObjectBuilder.LuaProperty> addPropertyRow = (prop) -> {
            HBox row = new HBox(6);
            TextField keyField = new TextField(prop != null ? prop.getKey() : "");
            keyField.setPromptText("Key");
            ComboBox<LuaObjectBuilder.PropertyType> typeBox = new ComboBox<>();
            typeBox.getItems().addAll(LuaObjectBuilder.PropertyType.values());
            typeBox.setValue(prop != null ? prop.getType() : LuaObjectBuilder.PropertyType.STRING);
            TextField valField = new TextField(prop != null ? prop.getValue() : "");
            valField.setPromptText("Value");
            HBox.setHgrow(valField, Priority.ALWAYS);

            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> {
                propertiesBox.getChildren().remove(row);
                generateLua.run();
            });

            keyField.textProperty().addListener((obs, oldV, newV) -> generateLua.run());
            typeBox.valueProperty().addListener((obs, oldV, newV) -> generateLua.run());
            valField.textProperty().addListener((obs, oldV, newV) -> generateLua.run());

            row.getChildren().addAll(keyField, typeBox, valField, removeBtn);
            propertiesBox.getChildren().add(row);
        };

        Consumer<LuaObjectBuilder.Template> applyTemplate = (tmpl) -> {
            LuaObjectBuilder builder = LuaObjectBuilder.createForTemplate(tmpl);
            varNameField.setText(builder.getVariableName());
            constructorField.setText(builder.getConstructorExpression());
            propertiesBox.getChildren().clear();
            for (LuaObjectBuilder.LuaProperty prop : builder.getProperties()) {
                addPropertyRow.accept(prop);
            }
            generateLua.run();
        };

        templateBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                applyTemplate.accept(newV);
            }
        });

        varNameField.textProperty().addListener((obs, oldV, newV) -> generateLua.run());
        constructorField.textProperty().addListener((obs, oldV, newV) -> generateLua.run());

        addPropertyBtn.setOnAction(e -> {
            addPropertyRow.accept(new LuaObjectBuilder.LuaProperty("propertyKey", "value", LuaObjectBuilder.PropertyType.STRING));
            generateLua.run();
        });

        generateBtn.setOnAction(e -> generateLua.run());

        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(luaPreviewArea.getText());
            clipboard.setContent(content);
            showInfo("Lua code copied to clipboard.");
        });

        addToCartridgeBtn.setOnAction(e -> {
            LuaObjectBuilder obj = buildCurrentObject.get();
            cartridgeObjects.add(obj);
            updateObjectsStatus();
            showInfo("Object '" + obj.getVariableName() + "' added to cartridge!\nTotal objects: " + cartridgeObjects.size());
        });

        applyTemplate.accept(LuaObjectBuilder.Template.ZONE);

        root.getChildren().addAll(templateRow, headerGrid, propHeader, propScrollPane, actionRow, new Label("Generated Lua Preview:"), luaPreviewArea);
        return root;
    }

    private VBox createDialogBuilderTab() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(8);
        headerGrid.setVgap(8);

        TextField nameField = new TextField("dialogSequence");
        headerGrid.add(new Label("Dialog Sequence Name:"), 0, 0);
        headerGrid.add(nameField, 1, 0);

        Label framesLabel = new Label("Initial Dialogue Frames (3 normal frames with text & media):");
        VBox framesBox = new VBox(6);

        Runnable addInitialFrameRow = () -> {
            HBox row = new HBox(6);
            int num = framesBox.getChildren().size() + 1;
            TextField textField = new TextField("Frame " + num + " dialogue text...");
            textField.setPromptText("Frame Text");
            HBox.setHgrow(textField, Priority.ALWAYS);
            TextField mediaField = new TextField("mediaFrame" + num);
            mediaField.setPromptText("Media Var");
            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> framesBox.getChildren().remove(row));
            row.getChildren().addAll(new Label("Frame " + num + ":"), textField, mediaField, removeBtn);
            framesBox.getChildren().add(row);
        };

        // Populate 3 default dialogue frames
        for (int i = 1; i <= 3; i++) {
            HBox row = new HBox(6);
            String text = switch (i) {
                case 1 -> "Welcome traveler! You have arrived at the mystical ancient ruins.";
                case 2 -> "Legend speaks of a hidden treasure buried beneath these stones.";
                default -> "Beware, for ancient guardians still watch over this sanctuary.";
            };
            String media = switch (i) {
                case 1 -> "mediaIntro";
                case 2 -> "mediaRuins";
                default -> "mediaGuardian";
            };
            TextField textField = new TextField(text);
            HBox.setHgrow(textField, Priority.ALWAYS);
            TextField mediaField = new TextField(media);
            Button removeBtn = new Button("X");
            removeBtn.setOnAction(e -> framesBox.getChildren().remove(row));
            row.getChildren().addAll(new Label("Frame " + i + ":"), textField, mediaField, removeBtn);
            framesBox.getChildren().add(row);
        }

        Button addFrameBtn = new Button("+ Add Frame");
        addFrameBtn.setOnAction(e -> addInitialFrameRow.run());

        GridPane qGrid = new GridPane();
        qGrid.setHgap(8);
        qGrid.setVgap(8);

        TextField qTextField = new TextField("Do you wish to enter the ruins and search for the treasure?");
        TextField qMediaField = new TextField("mediaQuestion");
        ComboBox<DialogBuilder.QuestionType> qTypeBox = new ComboBox<>();
        qTypeBox.getItems().addAll(DialogBuilder.QuestionType.values());
        qTypeBox.setValue(DialogBuilder.QuestionType.YES_NO);
        TextField choicesField = new TextField("Yes, No");
        TextField resultVarField = new TextField("varRuinsAccepted");

        qGrid.add(new Label("Question Text:"), 0, 0);
        qGrid.add(qTextField, 1, 0);
        qGrid.add(new Label("Question Media:"), 0, 1);
        qGrid.add(qMediaField, 1, 1);
        qGrid.add(new Label("Question Type:"), 0, 2);
        qGrid.add(qTypeBox, 1, 2);
        qGrid.add(new Label("Choices (comma separated):"), 0, 3);
        qGrid.add(choicesField, 1, 3);
        qGrid.add(new Label("Store Result in Variable:"), 0, 4);
        qGrid.add(resultVarField, 1, 4);

        TextArea luaPreview = new TextArea();
        luaPreview.setWrapText(true);
        VBox.setVgrow(luaPreview, Priority.ALWAYS);

        Supplier<DialogBuilder> buildCurrentDialog = () -> {
            DialogBuilder db = new DialogBuilder(nameField.getText());

            for (javafx.scene.Node node : framesBox.getChildren()) {
                if (node instanceof HBox row) {
                    TextField txtFld = (TextField) row.getChildren().get(1);
                    TextField medFld = (TextField) row.getChildren().get(2);
                    if (!txtFld.getText().isBlank()) {
                        db.addInitialFrame(txtFld.getText(), medFld.getText());
                    }
                }
            }

            List<String> choiceList = java.util.Arrays.stream(choicesField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

            db.setQuestion(qTextField.getText(), qMediaField.getText(), qTypeBox.getValue(), choiceList);
            db.setResultVariable(resultVarField.getText());

            if (qTypeBox.getValue() == DialogBuilder.QuestionType.YES_NO) {
                String btn1 = choiceList.size() > 0 ? choiceList.get(0) : "Yes";
                String btn2 = choiceList.size() > 1 ? choiceList.get(1) : "No";

                db.addBranchFrame(btn1, "You step bravely into the shadows of the ruin...", "mediaEnter");
                db.addBranchFrame(btn1, "A glowing chest appears in front of you! You found the treasure!", "mediaTreasure");

                db.addBranchFrame(btn2, "You decide to stay safe and turn back.", "mediaTurnBack");
                db.addBranchFrame(btn2, "Perhaps another day adventure will call again.", "mediaGoodbye");
            } else {
                for (String c : choiceList) {
                    db.addBranchFrame(c, "Follow-up response dialogue frame for: " + c, "mediaBranch_" + c.replaceAll("[^a-zA-Z0-9]", ""));
                }
            }

            return db;
        };

        Runnable generateLua = () -> {
            luaPreview.setText(buildCurrentDialog.get().toLuaCode());
        };

        Button generateBtn = new Button("Generate Lua Code");
        generateBtn.setOnAction(e -> generateLua.run());

        Button copyBtn = new Button("Copy to Clipboard");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(luaPreview.getText());
            clipboard.setContent(content);
            showInfo("Branching dialog Lua code copied to clipboard.");
        });

        Button addToCartridgeBtn = new Button("Add Dialog to Cartridge");
        addToCartridgeBtn.setOnAction(e -> {
            DialogBuilder db = buildCurrentDialog.get();
            LuaObjectBuilder builder = new LuaObjectBuilder(db.getName(), "-- Branching Dialogue Function");
            builder.addProperty("Code", db.toLuaCode(), LuaObjectBuilder.PropertyType.EXPRESSION);
            cartridgeObjects.add(builder);
            updateObjectsStatus();
            showInfo("Branching Dialog '" + db.getName() + "' added to cartridge!\nTotal objects: " + cartridgeObjects.size());
        });

        HBox actionRow = new HBox(8, generateBtn, copyBtn, addToCartridgeBtn);

        VBox treeVisualContainer = new VBox(6);
        treeVisualContainer.setPadding(new Insets(8));
        treeVisualContainer.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6;");

        Runnable updateTreeVisual = () -> {
            treeVisualContainer.getChildren().clear();
            treeVisualContainer.getChildren().add(new Label("Visual Dialogue Graph Flow (Branching & Merging Support):"));

            HBox flow = new HBox(12);
            flow.setPadding(new Insets(6));

            // Col 1: Initial Frames
            VBox colLeft = new VBox(6);
            colLeft.setPrefWidth(200);
            int idx = 1;
            for (javafx.scene.Node node : framesBox.getChildren()) {
                if (node instanceof HBox row) {
                    TextField txtFld = (TextField) row.getChildren().get(1);
                    TextField medFld = (TextField) row.getChildren().get(2);
                    VBox card = new VBox(4);
                    card.setPadding(new Insets(6));
                    card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #2563eb; -fx-border-width: 1 1 1 4; -fx-border-radius: 4; -fx-background-radius: 4;");
                    card.getChildren().addAll(
                        new Label("Frame " + (idx++) + " [Normal]"),
                        new Label("\"" + txtFld.getText() + "\""),
                        new Label("🖼️ " + medFld.getText())
                    );
                    colLeft.getChildren().add(card);
                }
            }

            Label arrow1 = new Label(" ➔ ");
            arrow1.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");

            // Col 2: Question Node
            VBox colMid = new VBox(6);
            colMid.setPrefWidth(200);
            VBox qCard = new VBox(4);
            qCard.setPadding(new Insets(6));
            qCard.setStyle("-fx-background-color: #fefce8; -fx-border-color: #d97706; -fx-border-width: 1 1 1 4; -fx-border-radius: 4; -fx-background-radius: 4;");
            qCard.getChildren().addAll(
                new Label("Question Node [" + qTypeBox.getValue() + "]"),
                new Label("\"" + qTextField.getText() + "\""),
                new Label("🖼️ " + qMediaField.getText()),
                new Label("Saved Var: " + resultVarField.getText())
            );
            colMid.getChildren().add(qCard);

            Label arrow2 = new Label(" ➔ ");
            arrow2.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");

            // Col 3: Choice Branch Nodes
            VBox colRight = new VBox(6);
            colRight.setPrefWidth(200);
            List<String> choicesList = java.util.Arrays.stream(choicesField.getText().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

            for (String choice : choicesList) {
                VBox bCard = new VBox(4);
                bCard.setPadding(new Insets(6));
                bCard.setStyle("-fx-background-color: #ffffff; -fx-border-color: #16a34a; -fx-border-width: 1 1 1 4; -fx-border-radius: 4; -fx-background-radius: 4;");
                bCard.getChildren().addAll(
                    new Label("Branch '" + choice + "'"),
                    new Label("Follow-up frame for " + choice),
                    new Label("➔ Next Node: Merged Path")
                );
                colRight.getChildren().add(bCard);
            }

            Label arrow3 = new Label(" ➔ ");
            arrow3.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");

            // Col 4: Recording Node & Merged Destination Node
            VBox colMerge = new VBox(6);
            colMerge.setPrefWidth(200);

            for (DialogBuilder.DialogNode dn : buildCurrentDialog.get().getNodes().values()) {
                if (dn instanceof DialogBuilder.RecordingNode rn) {
                    VBox recCard = new VBox(4);
                    recCard.setPadding(new Insets(6));
                    recCard.setStyle("-fx-background-color: #ecfdf5; -fx-border-color: #059669; -fx-border-width: 1 1 1 4; -fx-border-radius: 4; -fx-background-radius: 4;");
                    String icon = switch (rn.getRecording()) {
                        case SNAP_PHOTO -> "📷";
                        case PICK_FILE -> "📁";
                        case RECORD_VIDEO -> "📹";
                        case SCAN_BARCODE -> "🔍";
                        default -> "🎙️";
                    };
                    recCard.getChildren().addAll(
                        new Label("Recording Node [" + rn.getRecording() + "]"),
                        new Label(icon + " \"" + rn.getText() + "\""),
                        new Label("Result Var: " + rn.getResultMediaVar()),
                        new Label("Lua: " + (rn.getTargetLuaCode() != null ? rn.getTargetLuaCode() : "(none)"))
                    );
                    colMerge.getChildren().add(recCard);
                }
            }

            VBox mCard = new VBox(4);
            mCard.setPadding(new Insets(6));
            mCard.setStyle("-fx-background-color: #f3e8ff; -fx-border-color: #9333ea; -fx-border-width: 1 1 1 4; -fx-border-radius: 4; -fx-background-radius: 4;");
            mCard.getChildren().addAll(
                new Label("Merged Graph Node"),
                new Label("\"Common follow-up dialogue where branches converge.\""),
                new Label("🖼️ mediaTavern")
            );
            colMerge.getChildren().add(mCard);

            flow.getChildren().addAll(colLeft, arrow1, colMid, arrow2, colRight, arrow3, colMerge);
            treeVisualContainer.getChildren().add(flow);
        };

        generateBtn.setOnAction(e -> {
            generateLua.run();
            updateTreeVisual.run();
        });

        generateLua.run();
        updateTreeVisual.run();

        ScrollPane scroll = new ScrollPane(new VBox(10, headerGrid, framesLabel, framesBox, addFrameBtn, new Label("Question & Branching Configuration:"), qGrid, treeVisualContainer));
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320);

        root.getChildren().addAll(scroll, actionRow, new Label("Generated Branching Dialogue Lua Code:"), luaPreview);
        return root;
    }

    private void updateObjectsStatus() {
        if (cartridgeObjects.isEmpty()) {
            objectsStatusLabel.setText("Objects included: 0 objects");
        } else {
            List<String> names = cartridgeObjects.stream().map(LuaObjectBuilder::getVariableName).toList();
            objectsStatusLabel.setText("Objects included: " + cartridgeObjects.size() + " (" + String.join(", ", names) + ")");
        }
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
                parseDouble(createAltitudeField.getText(), 0.0),
                cartridgeObjects
            );
            showInfo("Template created:\n" + output
                + "\n\nIncludes " + cartridgeObjects.size() + " constructed objects.");
        } catch (IOException e) {
            showError("Failed to write template: " + e.getMessage());
        }
    }

    private void compileGwcCartridge() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Compiled Cartridge (.gwc)");
        chooser.setInitialFileName("cartridge.gwc");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wherigo Cartridge (*.gwc)", "*.gwc"));
        File selected = chooser.showSaveDialog(primaryStage);
        if (selected == null) {
            return;
        }
        try {
            CartridgeCompiler.CartridgeMetadata meta = new CartridgeCompiler.CartridgeMetadata(
                createNameField.getText(),
                createAuthorField.getText(),
                createVersionField.getText(),
                createDescriptionField.getText(),
                parseDouble(createLatitudeField.getText(), 0.0),
                parseDouble(createLongitudeField.getText(), 0.0),
                parseDouble(createAltitudeField.getText(), 0.0)
            );
            CartridgeCompiler.compileToGwcFile(meta, cartridgeObjects, selected);
            showInfo("Cartridge compiled successfully!\nFile: " + selected.getAbsolutePath()
                + "\nIncludes " + cartridgeObjects.size() + " constructed Lua objects.");
        } catch (Exception e) {
            showError("Failed to compile cartridge: " + e.getMessage());
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
