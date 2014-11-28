import com.vinumeris.crashfx.CrashFX;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A demo showing how to integrate CrashFX with your application.
 */
public class ReallyBuggyApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Firstly, let's get ourselves a directory to store crash reports across restarts. In a real app you would
        // use a directory in the appropriate OS-specific locations. If you also use UpdateFX, you can use the
        // AppDirectory class from that library to find a good, appropriate location. For this demo we just use /tmp
        Path tmp = Paths.get(System.getProperty("java.io.tmpdir"), "buggydemo");
        if (!Files.exists(tmp))
            Files.createDirectory(tmp);

        // Now we initialse CrashFX with a name for our app, a place to stash crash reports after a crash, and a URL
        // to upload them to. We could also just use CrashFX.setup() if all we care about is the dialog.
        CrashFX.setup("Really buggy demo 1.0", tmp, URI.create("http://localhost:8080/crashfx/upload"));

        Button crashMeForeground, crashMeBackground;
        VBox vBox = new VBox(
                (crashMeForeground = new Button("Crash me on the UI thread!")),
                (crashMeBackground = new Button("Crash me on a background thread!"))
        );
        vBox.setPadding(new Insets(30));
        vBox.setSpacing(30);
        vBox.setAlignment(Pos.CENTER);

        crashMeForeground.setOnAction(ev -> {
            throw new AssertionError("I just want to go to sleep ...");
        });
        crashMeBackground.setOnAction(ev -> {
            new Thread(() -> {
                CrashFX.uncheck(() -> {
                    // Would normally need to handle InterruptedException here because of the sleep, but uncheck()
                    // lets us ignore any thrown exceptions inside the lambda - they will be rethrown as
                    // RuntimeException for us.
                    Thread.sleep(200);

                    // Now trigger a divide by zero.
                    int a = 10 / 0;
                });
            }).start();
        });

        primaryStage.setScene(new Scene(vBox));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
