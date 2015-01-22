package com.vinumeris.crashfx;

import javafx.application.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.nio.file.*;
import java.time.*;
import java.time.format.*;

/**
 * UI controller for the window that shows up when there's a crash.
 */
public class CrashWindow {
    @FXML public Label messageLabel;
    @FXML public Label detailsLabel;
    @FXML public VBox imageVBox;
    @FXML public VBox contentVBox;
    @FXML public Button actionButton;
    @FXML public CheckBox uploadCheckBox;

    public String log;
    public Stage stage;

    /**
     * Opens the crash alert for the given throwable and then terminates the app once the user acknowledges. Can be
     * called from any thread.
     */
    public static void open(Throwable throwable) {
        // We ensure we are running this code from inside a runLater closure, because otherwise we may encounter an
        // internal error from JavaFX: "Nested event loops are allowed only while handling system events". That
        // can occur if the crash occurs during an animation.
        Platform.runLater(() -> {
            try {
                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                FXMLLoader loader = new FXMLLoader(CrashWindow.class.getResource("crash.fxml"));
                Pane root = loader.load();
                CrashWindow controller = loader.getController();

                String stackTrace = CrashFX.getStackTrace(throwable);
                CrashFX.LOGGER.accept(stackTrace);

                if (CrashFX.UPLOAD_URI == null) {
                    controller.uploadCheckBox.setSelected(false);
                    controller.uploadCheckBox.setVisible(false);
                }

                controller.log = String.format("Crash at %s%n%s%n%s%n%n%s", nowAsString(), CrashFX.APP_IDENTIFIER, stackTrace, CrashFX.getRecentLogs());
                controller.stage = dialogStage;
                Scene scene = new Scene(root);
                dialogStage.setScene(scene);
                dialogStage.showAndWait();
            } catch (Throwable e) {
                // We crashed whilst trying to show the alert dialog. This can happen if we're being crashed by inbound
                // closures onto the event thread which will execute in the nested event loop. Just give up here: at the
                // moment we have no way to filter them out of the event queue.
                CrashFX.LOGGER.accept("Crashed whilst showing the crash dialog, giving up");
                CrashFX.LOGGER.accept(CrashFX.getStackTrace(e));
                Runtime.getRuntime().exit(1);
            }
            // And now quit the app.
            Platform.exit();
        });
    }

    private static String nowAsString() {
        return Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    @FXML   // - annotation unnecessary but useful for ProGuard
    public void okClicked(ActionEvent event) {
        if (uploadCheckBox.isSelected()) {
            assert CrashFX.DIRECTORY != null;
            // File name is arbitrary. But don't put colons (i.e. formatted timestamps) in the file name! Windows
            // will barf at such a name.
            String filename = String.format("%d.crash.txt", Instant.now().getEpochSecond());
            CrashFX.ignoreAndLog(() -> Files.write(CrashFX.DIRECTORY.resolve(filename), log.getBytes()));
        }
        Platform.exit();
    }

    private boolean showingDetails = false;

    @FXML
    public void actionClicked(ActionEvent event) {
        if (!showingDetails) {
            TextArea area = new TextArea(log);
            VBox.setVgrow(area, Priority.ALWAYS);
            contentVBox.getChildren().add(area);
            stage.setHeight(stage.getHeight() + 200 + contentVBox.getSpacing());
            actionButton.setText("Copy to clipboard");
            showingDetails = true;
        } else {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(log);
            clipboard.setContent(content);
        }
    }
}
