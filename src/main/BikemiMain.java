package main;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import prediction.HistoricMeanPredictor;
import prediction.HistoricTrendPredictor;
import prediction.LastValuePredictor;
import prediction.MahoutPredictor;
import prediction.Predictor;

import mapreduce.BikeMiBinDriver;

import utility.DateUtil;


/**
 * Semplice classe in cui memorizziamo i valori in ingresso e implementiamo la logica dell'applicazione:
 * <li>trasformare il dataset bikeMi in un formato utile per la predizione;
 * <li>instanzia e allena 4 predittori;</li>
 * <li>stampa le performance dei 4 predittori</li>.
 * 
 * @author Armando Segatori
 *
 */
public class BikemiMain {

	/**
	 * Root path. Tutte le cartelle e i file necessari si trovano all'interno di questa directory
	 * Caricare i file di input nel HDFS a linea di comando
	 */
	//public static final String ROOT_PATH  = "hdfs://localhost:[PORT_NAME]/[PATH]/"; // Inserire il proprio percorso
	public static final String ROOT_PATH  = "hdfs://localhost:9000/master/bikemi/small_dataset/"; // Inserire il proprio percorso

	/**
	 * Pattern utilizzato per recuperare le informazioni (id, bike, free, timestamp) dai files caricati
	 * nel proprio HDFS
	 */
	private static final String SPLITTER 		= " ";
	
	/**
	 * Data di inizio da cui partire per allenare i propri predittori
	 */
	private static final String START_DATE 		= "2013-06-07T00:00:00.000000";
	
	/**
	 * Numeri di giorni da considerare
	 */
	private static final int 	DAYS 			= 14; // TOT 14 (From 6 June to 20 June)
	
	/**
	 * Dimensione del singolo bin in minuti
	 */
	private static final int	MINUTE_INTERVAL = 15;
	
	/**
	 * Dimensione del singolo bin in millisecondi
	 */
	private static final long	MILLS_INTERVAL 	= MINUTE_INTERVAL * 60 * 1000;
	
	/**
	 * Numero dei possibili valori che può assumere la variabile di uscita
	 */
	private static final int 	NUM_TARGET 		= 4;
	
	/**
	 * Finestra di predizione (Prediction Window)
	 */
	private static final int 	PW				= 2;
	
	/**
	 * Numero di bins in un giorno
	 */
	private static final int 	BINS_PER_DAY	= 1440 / MINUTE_INTERVAL; // day_minutes / bin_minutes
	
	/**
	 * Directory in cui è memorizzati il proprio dataset
	 */
	private static final String INPUT_ROOT_PATH = ROOT_PATH + "input/june";

	/**
	 * Entry point dell'applicazione.
	 * 
	 * @param args parametri in ingresso passati a linea di comando
	 * @throws Exception  
	 */
	public static void main(String[] args) throws Exception {
	
		// Cast date string to a data class
		Date startDate = DateUtil.stringIso8601ToDate(START_DATE); 
		// Call the job to transform the dataset
		// BikeMiBinDriver.runJob(SPLITTER, startDate, DAYS, MILLS_INTERVAL, NUM_TARGET, INPUT_ROOT_PATH);

		// Create the predictors list
		List<Predictor> preidctors = new ArrayList<Predictor>();
		preidctors.add(new MahoutPredictor(NUM_TARGET, PW)); // Add the Mahout Predictor Wrapper to the list
		preidctors.add(new LastValuePredictor(NUM_TARGET, PW)); // Add the Last Value Predictor to the list
		preidctors.add(new HistoricMeanPredictor(NUM_TARGET, PW)); // Add the Historic Mean Predictor to the list
		preidctors.add(new HistoricTrendPredictor(NUM_TARGET, PW)); // Add the Historic Trend Predictor to the list

		// For each predictor, train it and print performance
		StringBuilder sb = new StringBuilder("Performance:\n");
		for (Predictor predictor : preidctors){
			// Train predictor
			predictor.buildClassifier(BikeMiBinDriver.getOutputPath(), 
											BikeMiBinDriver.SPLITTER_BIN, DAYS, BINS_PER_DAY, NUM_TARGET);
			// Get predictor performance
			sb.append(predictor.printInfo() + "\n\n");
		}

		System.out.println(sb.toString());

	}

}
