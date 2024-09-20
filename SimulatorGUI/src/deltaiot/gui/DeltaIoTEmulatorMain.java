package deltaiot.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.SimpleAdaptation;
import simulator.QoS;
import simulator.Simulator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import javafx.scene.chart.LineChart;

public class DeltaIoTEmulatorMain extends Application {
	@FXML
	private Button runEmulator, btnSaveResults, btnAdaptationLogic, btnClearResults, btnDisplay;

	@FXML
	private LineChart<?, ?> chartPacketLoss, chartEnergyConsumption;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private Label lblProgress;

	public static final Path filePath = Paths.get(System.getProperty("user.dir") + "/data/data.csv");
	List<String> data = new LinkedList<String>();
	Stage primaryStage;
	Simulator simul;
	Service serviceEmulation = new Service() {

		@Override
		protected void succeeded() {
			displayData("Without Adaptation", 0);
		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					btnDisplay.setDisable(true);
					simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT();
					//simul = Simulator.createBaseCase();

					for (int i = 0; i < 96; i++)
						simul.doSingleRun();
					btnDisplay.setDisable(false);
					return null;
				}
			};
		}
	};

	@FXML
	void runEmulatorClicked(ActionEvent event) {
		if (!serviceEmulation.isRunning()) {
			serviceEmulation.restart();
			serviceProgress.restart();
		}
	}

	Service serviceProgress = new Service() {
		@Override
		protected void succeeded() {

		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					int run;
					do {
						run = simul.getRunInfo().getRunNumber();

						updateProgress(run, 96);
						updateMessage("(" + run + "/96" + ")");

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					} while (run < 96);
					
					return null;
				}
			};
		}
	};

	Service serviceAdaptation = new Service() {

		@Override
		protected void succeeded() {
			displayData("With Adaptation", 1);
		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					btnDisplay.setDisable(true);
					SimpleAdaptation client = new SimpleAdaptation();
					client.start();
					simul = client.getSimulator();
					btnDisplay.setDisable(false);
					return null;
				}
			};
		}
	};

	// ActivFORMSDeploy client;

	@FXML
	void btnAdaptationLogic(ActionEvent event) {
		if (!serviceAdaptation.isRunning()) {
			serviceAdaptation.restart();
			serviceProgress.restart();
		}
	}

	@FXML
	void btnDisplay(ActionEvent event) {
		try {
			//URL url = new URL("file://" + System.getProperty("user.dir") + "/resources/DeltaIoTModel.fxml");
			URL url = new URL("file:///C:/Users/vlrbr/Downloads/72-artifact/72-artifact/DeltaIoT-executables/Simulation/resources/DeltaIoTModel.fxml");

			FXMLLoader fxmlLoader = new FXMLLoader(url);
			Parent root1 = (Parent) fxmlLoader.load();
			DeltaIoTClientMain main = fxmlLoader.getController();
			main.setSimulationClient(simul);
			Stage stage = new Stage();
			stage.setScene(new Scene(root1));
			stage.setResizable(false);
			stage.setTitle("DeltaIoT Topology");
			stage.setAlwaysOnTop(true);
			stage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	void initialize() {
		assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'Progress.fxml'.";
		lblProgress.textProperty().bind(serviceProgress.messageProperty());
		progressBar.progressProperty().bind(serviceProgress.progressProperty());
	}

	@FXML
	void btnSaveResults(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		// Set extension filter
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
		fileChooser.getExtensionFilters().add(extFilter);

		// Show save file dialog
		File file = fileChooser.showSaveDialog(primaryStage);

		if (file != null) {
			saveFile(file);
		}
	}

	private void saveFile(File file) {
		try {
			FileWriter fileWriter = null;

			fileWriter = new FileWriter(file, true);

			for (String line : data)
				fileWriter.write(line + "\n");
			fileWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@FXML
	void btnClearResults(ActionEvent event) {
		chartEnergyConsumption.getData().clear();
		chartPacketLoss.getData().clear();
		data.clear();
	}

	void displayData(String setName, int index) {
		XYChart.Series energyConsumptionSeries = new XYChart.Series();
		XYChart.Series packetLossSeries = new XYChart.Series();
		energyConsumptionSeries.setName(setName);
		packetLossSeries.setName(setName);
		List<QoS> qosList = simul.getQosValues();

		for (QoS qos : qosList) {
			data.add(qos + ", " + setName);
			energyConsumptionSeries.getData()
					.add(new XYChart.Data(Integer.parseInt(qos.getPeriod()), qos.getEnergyConsumption()));
			packetLossSeries.getData().add(new XYChart.Data(Integer.parseInt(qos.getPeriod()), qos.getPacketLoss()));
		}
		chartEnergyConsumption.getData().add(energyConsumptionSeries);
		chartPacketLoss.getData().add(packetLossSeries);
	}

	@Override
	public void start(Stage stage) throws Exception {
		//URL url = new URL("file://" + System.getProperty("user.dir") + "/SimulatorGUI/resources/EmulatorGUI.fxml");
		URL url = new URL("file:///C:/Users/vlrbr/Downloads/72-artifact/72-artifact/DeltaIoT-executables/Simulation/resources/EmulatorGUI.fxml");


		System.out.println(url);
		Parent root = FXMLLoader.load(url);// getClass().getResource(System.getProperty("user.dir")
											// +
											// "/resources/EmulatorGUI.fxml"));
		primaryStage = stage;
		Scene scene = new Scene(root, 900, 600);

		stage.setTitle("DeltaIoT");
		stage.setOnCloseRequest(new EventHandler() {

			@Override
			public void handle(Event arg0) {
				Platform.exit();
			}

		});
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
