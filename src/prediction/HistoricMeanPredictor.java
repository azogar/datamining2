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
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

public class HistoricMeanPredictor extends AbstractPredictor {


	private static final String NAME = "Historic Mean Value Regressor";
	
	/**
	 * Indice del vettore delle transazioni in cui è memorizzato il binId
	 */
	private final int BIN_ID_INDEX_TRANSACTION = 1;

	private HashMap<Integer, HashMap<Integer, Record>> dataMean;
	
	/**
	 * Costruttore 
	 * 
	 * @param numCategories numero di valori che la variabile di uscita può assumere
	 * @param pw finestra di predizione
	 */
	public HistoricMeanPredictor(int numCategories, int pw){
		super(numCategories, pw);
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
		cm = new ConfusionMatrix(symbols, "unknown");

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
	 * (intero tra 0 e {@link AbstractPredictor#getNumategories()}-1).<br>
	 * Considera il numero di biciclette disponibili costante durante la finestra
	 * di predizione pw.
	 * 
	 * @params vettore da classificare. Il vettore deve avere dimensione 5 e rappresenta
	 * 			la transazione. Esso contiene (in ordine): 
	 * 		<li>bike_station_id</li>
	 * 		<li>daily_bin_id</li>
	 * 		<li>available_bike_average</li>
	 * 		<li>bike_station_size</li>
	 * 		<li>category_id</li>
	 * 		L'unica informazione utilizzata è <li>category_id</li> che rappresenta il valore
	 * 		della variabile da predire al tempo t0. Il suo valore viene restituito come 
	 *		valore della variabile da predire al tempo t0+pw.
	 * 
	 * @return il valore della variabile da predire
	 */
	@Override
	public int classify(Vector vector) {
		int bikeId = (int) vector.get(0); // get the id of the bike station
		int binId = (int) vector.get(1); // get the id of the bike station
		
		return this.dataMean.get(bikeId).get(binId + pw).getSet();
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
	
	private class Record {
		public double sum = 0;
		public int count = 0;
		
		public void add(double value, double max) {
			sum += (value / max);
			count++;
		}
		
		public int getSet() {
			double coverage = sum / count;
			coverage *= HistoricMeanPredictor.this.numCategories;
			
			for(int i = 0; i < HistoricMeanPredictor.this.numCategories; i++)
			    if(coverage < 1d * i + 1d) return i;
			return HistoricMeanPredictor.this.numCategories - 1;
		}
	}
	

	@Override
	public void buildClassifier(Path input, String pattern, int days,int binsPerDay, int numTarget) 
			throws IOException {

		input = input.suffix("/part-r-00000");

		Pattern splitter =  Pattern.compile(pattern);

		// Holds Features
		HashMap<Integer, List<Vector>> dataTest = new HashMap<Integer, List<Vector>>();
		dataMean  = new HashMap<Integer, HashMap<Integer, Record>>();

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

			int bikeId = Integer.parseInt(values[0]); // get the id of the bike station
	
			if (dayId == days-1){ // Test set
				List<Vector> vectorBikeIdList = dataTest.get(bikeId);
				if (vectorBikeIdList == null)
					vectorBikeIdList = new ArrayList<Vector>();

				List<String> transaction = new ArrayList<String>(new ArrayList<String>(Arrays.asList(values)));
				vectorBikeIdList.add(getVectorFromDataTransaction(transaction));
				dataTest.put(bikeId, vectorBikeIdList); // add transaction to the test set
			} else {
				int binId = Integer.parseInt(values[1]); // get the id of the bike station
				if (dataMean.get(bikeId) == null)
					dataMean.put(bikeId, new HashMap<Integer, Record>());
		
				if (dataMean.get(bikeId).get(binId) == null)
					dataMean.get(bikeId).put(binId, new Record());
				
				dataMean.get(bikeId).get(binId).add(Double.parseDouble(values[2]), Integer.parseInt(values[3]));
			}

		}

		reader.close();

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
