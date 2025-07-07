package com.ppi.utility.importer;

import com.ppi.utility.importer.service.ExcelService;
import com.ppi.utility.importer.service.GenerateReportService; // Import the new service
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * JavaFX Controller for the main-view.fxml.
 * Handles UI interactions, file selection, and delegates Excel processing and report generation to respective services.
 */
@Component
@Scope("prototype")
public class MainController {

    @FXML
    private Button uploadButton;
    @FXML
    private Button submitButton;
    @FXML
    private Label messageLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    // New UI elements for report generation
    @FXML
    private Label fileNameInputLabel;
    @FXML
    private TextField fileNameInput;
    @FXML
    private Button getReportButton;

    private final ExcelService excelService;
    private final GenerateReportService generateReportService; // New: Injected GenerateReportService
    private File selectedExcelFile;

    @Autowired
    public MainController(ExcelService excelService,
                        GenerateReportService generateReportService) { // New: Added to constructor
        this.excelService = excelService;
        this.generateReportService = generateReportService; // New: Assigned
    }

    @FXML
    public void initialize() {
        resetUI();
    }

    /**
     * Resets the UI to its initial state (only "Upload File" button visible).
     */
    private void resetUI() {
        uploadButton.setText("Upload File");
        uploadButton.setVisible(true);
        uploadButton.setManaged(true);

        submitButton.setVisible(false);
        submitButton.setManaged(false);

        fileNameInputLabel.setVisible(false);
        fileNameInputLabel.setManaged(false);
        fileNameInput.setVisible(false);
        fileNameInput.setManaged(false);
        fileNameInput.clear(); // Clear any previous input
        getReportButton.setVisible(false);
        getReportButton.setManaged(false);

        messageLabel.setText("Click 'Upload File' to select an Excel document.");
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().remove("processing-message");

        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        selectedExcelFile = null;
    }

    /**
     * Updates the UI after a file has been selected for upload.
     *
     * @param file The selected Excel file.
     */
    private void updateFileSelectedUI(File file) {
        uploadButton.setText("Replace File");
        uploadButton.setVisible(true);
        uploadButton.setManaged(true);

        submitButton.setVisible(true);
        submitButton.setManaged(true);

        fileNameInputLabel.setVisible(false); // Hide report elements
        fileNameInputLabel.setManaged(false);
        fileNameInput.setVisible(false);
        fileNameInput.setManaged(false);
        getReportButton.setVisible(false);
        getReportButton.setManaged(false);

        DecimalFormat df = new DecimalFormat("#.##");
        String fileSize = df.format((double) file.length() / (1024 * 1024)); // Size in MB
        messageLabel.setText("Selected file: " + file.getName() + " (" + fileSize + " MB)");
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().remove("processing-message");

        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
    }

    /**
     * Updates the UI to show report generation options.
     */
    private void showReportUI() {
        uploadButton.setVisible(false); // Hide upload/submit buttons
        uploadButton.setManaged(false);
        submitButton.setVisible(false);
        submitButton.setManaged(false);

        fileNameInputLabel.setVisible(true); // Show report elements
        fileNameInputLabel.setManaged(true);
        fileNameInput.setVisible(true);
        fileNameInput.setManaged(true);
        getReportButton.setVisible(true);
        getReportButton.setManaged(true);

        messageLabel.setText("Enter the file name to generate a report.");
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().remove("processing-message");

        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
    }

    @FXML
    private void onUploadButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedExcelFile = file;
            updateFileSelectedUI(selectedExcelFile);
        } else {
            if (selectedExcelFile == null) {
                resetUI();
            }
        }
    }

    @FXML
    private void onSubmitButtonClick() {
        if (selectedExcelFile == null) {
            messageLabel.setText("Please select an Excel file first.");
            messageLabel.getStyleClass().add("error-message");
            return;
        }

        // Update UI for processing state
        uploadButton.setVisible(false);
        uploadButton.setManaged(false);
        submitButton.setVisible(false);
        submitButton.setManaged(false);

        messageLabel.setText("Processing " + selectedExcelFile.getName() + "...");
        messageLabel.getStyleClass().add("processing-message");
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().remove("error-message");

        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        Task<String> processTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return excelService.processExcelFile(selectedExcelFile);
            }

            @Override
            protected void succeeded() {
                String resultMessage = getValue();
                messageLabel.setText(resultMessage);
                if (resultMessage.startsWith("Upload successful")) {
                    messageLabel.getStyleClass().add("success-message");
                    messageLabel.getStyleClass().remove("error-message");
                } else {
                    messageLabel.getStyleClass().add("error-message");
                    messageLabel.getStyleClass().remove("success-message");
                }
                messageLabel.getStyleClass().remove("processing-message");

                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);

                // After successful upload and message display, transition to report UI
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(event -> showReportUI()); // Show report UI after delay
                pause.play();
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                String errorMessage = "Error: " + (exception != null ? exception.getMessage() : "Unknown error.");
                messageLabel.setText(errorMessage);
                messageLabel.getStyleClass().add("error-message");
                messageLabel.getStyleClass().remove("success-message");
                messageLabel.getStyleClass().remove("processing-message");
                System.err.println("Error processing Excel file: " + errorMessage);
                if (exception != null) {
                    exception.printStackTrace();
                }

                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);

                // After error, return to initial "Upload File" state after delay
                PauseTransition pause = new PauseTransition(Duration.seconds(5));
                pause.setOnFinished(event -> resetUI());
                pause.play();
            }
        };

        new Thread(processTask).start();
    }

    /**
     * Handles the action when the "Get Report" button is clicked.
     */
    @FXML
    private void onGetReportButtonClick() {
        String fileName = fileNameInput.getText().trim();

        if (fileName.isEmpty()) {
            messageLabel.setText("Please enter a file name for the report.");
            messageLabel.getStyleClass().add("error-message");
            return;
        }

        // Update UI for report generation processing state
        fileNameInputLabel.setVisible(false);
        fileNameInputLabel.setManaged(false);
        fileNameInput.setVisible(false);
        fileNameInput.setManaged(false);
        getReportButton.setVisible(false);
        getReportButton.setManaged(false);

        messageLabel.setText("Generating report for '" + fileName + "'...");
        messageLabel.getStyleClass().add("processing-message");
        messageLabel.getStyleClass().remove("success-message");
        messageLabel.getStyleClass().remove("error-message");

        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        Task<Void> reportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Call the new GenerateReportService
                byte[] excelBytes = generateReportService.generateReportExcel(fileName);

                // Determine Downloads folder path
                String userHome = System.getProperty("user.home");
                File downloadsDir = new File(userHome, "Downloads");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs(); // Create Downloads directory if it doesn't exist
                }

                String outputFileName = "report_" + fileName.replace(".xlsx", "").replace(".xls", "") + ".xlsx";
                File outputFile = new File(downloadsDir, outputFileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(excelBytes);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                messageLabel.setText("Download Successful! Report saved to Downloads folder.");
                messageLabel.getStyleClass().add("success-message");
                messageLabel.getStyleClass().remove("error-message");
                messageLabel.getStyleClass().remove("processing-message");

                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);

                // Reset UI to initial state after successful download
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(event -> resetUI());
                pause.play();
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                String errorMessage = "Error generating report: " + (exception != null ? exception.getMessage() : "Unknown error.");
                messageLabel.setText(errorMessage);
                messageLabel.getStyleClass().add("error-message");
                messageLabel.getStyleClass().remove("success-message");
                messageLabel.getStyleClass().remove("processing-message");
                System.err.println("Error generating report: " + errorMessage);
                if (exception != null) {
                    exception.printStackTrace();
                }

                progressIndicator.setVisible(false);
                progressIndicator.setManaged(false);

                // Reset UI to initial state after failed download
                PauseTransition pause = new PauseTransition(Duration.seconds(5));
                pause.setOnFinished(event -> resetUI());
                pause.play();
            }
        };

        new Thread(reportTask).start();
    }
}
