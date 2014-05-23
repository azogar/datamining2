package prediction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.classifier.ConfusionMatrix;
import org.apache.mahout.classifier.sgd.L2;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import utility.BinUtil;

/**
 * Predittore che utilizza il LogistiRegression di Mahout.
 * Costruisce tanti predittori quante sono i bike station.
 * 
 * @author Armando Segatori
 *
 */
public class MahoutPredictor extends AbstractPredictor{

	/**
	 * Nome del predittore
	 */
	private static final String NAME = "Mahout Regressor";
	
	/**
	 * Numero di feature utilizzate per la predizione
	 */
	private final int FEATURE_SIZE = 3;
	
	/**
	 * Indice del vettore delle transazioni in cui è memorizzato il binId
	 */
	private final int BIN_ID_INDEX_TRANSACTION = 1;

	/**
	 * Map che contiene i predittori. Uno per ogni bike station.
	 */
	private HashMap<Integer, OnlineLogisticRegression> lrMap = null;

	/**
	 * Costruttore 
	 * 
	 * @param numCategories numero di valori che la variabile di uscita può assumere
	 * @param pw finestra di predizione
	 */
	public MahoutPredictor(int numCateogries, int pw){
		super(numCateogries, pw);
		lrMap = new HashMap<Integer, OnlineLogisticRegression>();
	}

	/**
	 * Lista di simboli utilizzati per la predizione con cui settare la matrice di confusione.
	 * I simboli utilizzati sono degli interi da 0 a NUM_TARGET -1
	 * 
	 * @return lista di simboli utilizzati per la predizione. 
	 */
	private List<String> getSymbols(){
		List<String> symbols = new ArrayList<String>();
		for (int i = 0; i < numCategories; i++)
			symbols.add("" + i);

		return symbols;
	}

	/**
	 * Valuta le prestazioni del modello appena allenato
	 * 
	 * @param dataTest map che contiene tutte le transazioni da classificare
	 * 					per ogni bike station
	 */
	private void evaluate(HashMap<Integer, List<Vector>> dataTest){		
		List<String> symbols = getSymbols();
		cm = new ConfusionMatrix(symbols, "unknown"); // create the confusion matrix
		
		// For each bike station
		for (Entry<Integer, List<Vector>> entry : dataTest.entrySet()){
			List<Vector> currentVectorId = entry.getValue(); // get the transactions to classify
			for (int k = 0; k < currentVectorId.size()-pw; k++){ // for each transaction
				Vector vectorK = currentVectorId.get(k); // get the vector of the transaction
				int classifiedLabel = classify(vectorK); // cassify the transaction
				if (classifiedLabel == -1) // if -1 something classification is not ok
					continue;
			
				// Get the right vector k + pw
				Vector vectorKPw = getVectorKPw(k, pw, (int)vectorK.get(BIN_ID_INDEX_TRANSACTION), currentVectorId); 
				int correctLabel = (int)vectorKPw.get(vectorKPw.size()-1); // get the right value of the outcome
				cm.addInstance("" + correctLabel, "" + classifiedLabel); // update confusion matrix
			}
		}
	}
	
	/**
	 * Trasforma il vettore che contiene la transazione in un vettore che contiene
	 * solamente le informazioni utilizzate per la predizione (feature).<br>
	 * Il vettore delle transazione ha il seguente formato:<br>
	 * [bike_station_id, bin_id, average, size, outcome_value]<br>
	 * Il vettore delle features ha il seguente formato:<br>
	 * [bin_id, average, size]<br>
	 * 
	 * @param transactionVector vettore che contiene la transazione
	 * @return vettore che contiene solamente le feature
	 */
	private Vector getFeatureVector(Vector transactionVector){
		Vector vector = new DenseVector(FEATURE_SIZE);
		for (int i = 0; i < FEATURE_SIZE; i++)
			vector.set(i, transactionVector.get(i+1));
		
		return vector;
	}
	
	/**
	 * Il metodo è lo stesso di {@link #getFeatureVector} con la differenza che trasforma
	 * l'id del bin in un bin id giornaliero.
	 * 
	 * @param transactionVector vettore che contiene la transazione.
	 * @param binsPerDay numero di bins in un giorno.
	 * @return vettore che contiene solamente le feature
	 */
	private Vector getFeatureVector(Vector transactionVector, int binsPerDay){
		transactionVector.set(BIN_ID_INDEX_TRANSACTION, 
								BinUtil.getBinIdFromUniqueBinIdInAllDays(binsPerDay, 
										(int)transactionVector.get(BIN_ID_INDEX_TRANSACTION)));
		
		return getFeatureVector(transactionVector);
	}

	/**
	 * Recupera il vettore delle transazioni traslato di PW. Per controllare che sia 
	 * corretto la differenza tra l'identificatore del bin del vettore al tempo t0,
	 * <i>vettore k</i> e l'identificatore del bin del vettore al tempo t0+pw, 
	 * <i>vettore k+pw</i> deve essere pari a pw.
	 * 
	 * @param k l'indice del <i>vettore k</i>, ossia il vettore al tempo t0, all'interno della
	 * 			lista <code>currentVector</code>.
	 * @param pw finestra di predizione per recuperare il <i>vettore k+pw</i>, ossia il vettore  tempo t0+pw
	 * @param binId id del bin dele vettore k
	 * @param currentVector lista in cui cercare il <i>vettore k+pw</i>
	 * @return il <i>vettore k+pw</i> o null se non trovato 
	 */
	private Vector getVectorKPw(int k, int pw, int binId, List<Vector> currentVector){
		Vector vectorKPw = null;
		for (int i = k + pw; i > k; i--){
			vectorKPw = currentVector.get(i);
			if (vectorKPw.get(BIN_ID_INDEX_TRANSACTION) == binId+pw) 
				return vectorKPw;
			
		}
		
		return null;
	}

	/**
	 * Allena tanti modelli quanti sono i bike station.
	 * Per ogni feature al tempo t0 utilizza come variabile di predizione
	 * quella al tempo t0+pw.
	 * 
	 * @param dataTraining map che contiene per ogni bike station la lista del training set
	 * @param binsPerDay numero di bin in un giorno
	 */
	private void train(HashMap<Integer, List<Vector>> dataTraining, int binsPerDay){
		// For each bike station
		for (Entry<Integer, List<Vector>> entry : dataTraining.entrySet()){
			List<Vector> currentVectorId = entry.getValue();
			// create the regression
			OnlineLogisticRegression lr = 
					new OnlineLogisticRegression(numCategories, FEATURE_SIZE, new L2());
			// For each transaction
			for (int k = 0; k < currentVectorId.size()-pw; k++){
				Vector vectorK = currentVectorId.get(k); // get the vector k
				// Get the right vector k+pw
				Vector vectorKPw = getVectorKPw(k, pw, (int)vectorK.get(BIN_ID_INDEX_TRANSACTION), currentVectorId); 
				// if null the vector k+pw not exist
				if (vectorKPw == null)
					continue;
				
				// Get the right value for the outcome
				int actual = (int) vectorKPw.get(vectorKPw.size()-1); // get the last index (the class label)
				// Train the model
				lr.train(actual, getFeatureVector(vectorK, binsPerDay));
			}
			
			// Put in the map the trained model
			lrMap.put(entry.getKey(), lr); 
			
		}

	}

	/**
	 * Trasforma una lista di stringhe in un vettore compatibile con Mahout
	 * 
	 * @param data lista da trasformare
	 * @return vector vettore che contiene gli stessi elementi della lista allo stesso indice
	 */
	private Vector getVectorFromDataTransaction(List<String> data){
		Vector vec = new DenseVector(data.size());
		int i = 0;
		for (String value : data)
			vec.set(i++, Double.parseDouble(value));

		return vec;
	}

	/**
	 * Classifica il vettore e ritorna il valore della variabile di uscita 
	 * (intero tra 0 e {@link AbstractPredictor#getNumategories()}-1).
	 * 
	 * @params vettore da classificare. Il vettore deve avere dimensione 5 e rappresenta
	 * 			la transazione. Esso contiene (in ordine): 
	 * 		<li>bike_station_id</li>
	 * 		<li>daily_bin_id</li>
	 * 		<li>available_bike_average</li>
	 * 		<li>bike_station_size</li>
	 * 		<li>category_id</li>
	 * 		Il <i>bike_station_id</i> è utilizzato per recuperare il corretto predittore.<br>
	 * 		<li>category_id</li> non è utilizzato.
	 * 
	 * @return il valore della variabile di predire o -1 in caso di errore
	 */
	@Override
	public int classify(Vector vector) {
		// Get the righ online logistic regression
		OnlineLogisticRegression lr = lrMap.get(new Integer((int)vector.get(0))); // Value in position 0 contains the bike_station_id
		if (lr == null){
			return -1; 
		}
		// Classify
		Vector result =  lr.classifyFull(getFeatureVector(vector));
		// Return the index with max value
		return result.maxValueIndex();
	}

	/**
	 * Ordina la lista per ogni bike station in base al bin id.
	 * 
	 * @param data map da ordinare
	 */
	private void sort(HashMap<Integer, List<Vector>> data){
		// Define the comparator
		Comparator<Vector> comparator = new Comparator<Vector>(){
			@Override
			public int compare(Vector o1, Vector o2) {
				return new Double(o1.get(BIN_ID_INDEX_TRANSACTION)).compareTo(new Double(o2.get(BIN_ID_INDEX_TRANSACTION)));
			}

		};
	
		// For each bike station
		for (Entry<Integer, List<Vector>> entry : data.entrySet()){
			int id = entry.getKey();
			List<Vector> currentVectorId = entry.getValue();
			Collections.sort(currentVectorId, comparator); // sort the list
			data.put(id, currentVectorId);
		}
	}
	
	
	@Override
	public void buildClassifier(Path input, String pattern, int days, 
			int binsPerDay, int numTarget) 
			throws IOException {
		
		input = input.suffix("/part-r-00000");

		Pattern splitter =  Pattern.compile(pattern);

		// Holds Features for the training set
		HashMap<Integer, List<Vector>> dataTraining = new HashMap<Integer, List<Vector>>();
		// Holds Features for the test set
		HashMap<Integer, List<Vector>> dataTest = new HashMap<Integer, List<Vector>>();

		////////////////////////////////////////////////////////
		// 		 Reads data and set the parameters 			  //
		////////////////////////////////////////////////////////
		Configuration conf = new Configuration();
		// Recupero il filesystem utilizzato
		FileSystem fs = FileSystem.get(input.toUri(), conf);
		// Read data
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, input, conf);
		IntWritable key = new IntWritable();
		Text value = new Text();
		while (reader.next(key, value)){
			String[] values = splitter.split(value.toString());
			int dayId = key.get();
			List<String> transaction = null;
			int bikeId = Integer.parseInt(values[0]); // get the id of the bike station
			if (dayId == days-1){ // Test set
				List<Vector> vectorBikeIdList = dataTest.get(bikeId);
				if (vectorBikeIdList == null)
					vectorBikeIdList = new ArrayList<Vector>();

				transaction = new ArrayList<String>(new ArrayList<String>(Arrays.asList(values)));
				vectorBikeIdList.add(getVectorFromDataTransaction(transaction));
				dataTest.put(bikeId, vectorBikeIdList); // add transaction to the test set
			}
			else{ // Training set
				// get list of index
				List<Vector> vectorBikeIdList = dataTraining.get(bikeId);
				if (vectorBikeIdList == null)
					vectorBikeIdList = new ArrayList<Vector>();
				
				values[1] = "" + BinUtil.getUniqueBinIdInAllDays(dayId, binsPerDay, Integer.parseInt(values[1]));
				transaction = new ArrayList<String>(new ArrayList<String>(Arrays.asList(values)));;
				vectorBikeIdList.add(getVectorFromDataTransaction(transaction));
				dataTraining.put(bikeId, vectorBikeIdList); // add transaction to the training set
			}

		}

		reader.close();

		sort(dataTraining); // sort the training set
		train(dataTraining, binsPerDay); // train the models

		sort(dataTest); // sort the test set
		evaluate(dataTest); // test the model

	}
	
	@Override
	public String printInfo() {
		StringBuilder sb = new StringBuilder(NAME + ":\n");
		sb.append(super.printInfo());
		return sb.toString();
	}	

}
