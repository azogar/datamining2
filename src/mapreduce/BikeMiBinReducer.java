package mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import utility.BinUtil;

/**
 * Il Reducer riceve le informazioni inviate dal Mapper e si occupa di calcolare i valori corretti da 
 * scrivere nel dataset risultante. Per maggiori informazioni sulla struttura del dataset vedere la javadoc 
 * di {@link BikeMiBinDriver}.<br>
 * Dal momento che per un bin possiamo avere più osservazioni, il reducer calcola il numero di biciclette disponibili
 * in un determinato bin per un determinato bike station, come la media <i>average</i> tra tutti posti disponibili letti
 * in ogni transazione. Inoltre, i dati letti possono variare significativamete da transazione a transazione a causa di
 * dati corrotti o malfunzionamenti dei sensori installati sui bike station. Ad esempio per un bike station le transazioni 
 * relative ad un determinato bin possono avere valori differenti per il campo <i>size</i>. Per questo motivo il Reducer 
 * calcola la dimensione del bike station con la stessa dimensione che ha la maggiore frequenza tra tutte le osservazioni lette 
 * in quel determinato bin. Solamente queste transazioni sono prese in considerazione per il calcolo della media.
 * 
 * @author Armando Segatori
 * @see BikeMiBinDriver
 */
public class BikeMiBinReducer extends Reducer<Text, Text, IntWritable, Text>{

	/**
	 * Millisecondi in un giorno
	 */
	private static final long MILLS_IN_A_DAY = 86400000; //  24 (hors) * 60 (minutes) * 60 (seconds) * 1000 (milliseconds);

	/**
	 * Soglia che definisce quando una percentuale appartiene ad un'etichetta piuttosto che ad un'altra
	 */
	private double numClassLabelTreshold;
	
	/**
	 * Numero di valori che la variabile di uscita può assumere.
	 */
	private int numTarget;
	
	/**
	 * Numero di bins in un giorno.
	 */
	private int binsPerDay;

	private IntWritable wInt = new IntWritable();
	private Text wText = new Text();

	/**
	 * Recupera la dimensione del bike station. Per vari motivi, quali malfunzinamento
	 * dei sensori installati sui singoli stalli del bike station o un errato invio
	 * dei dati durante la rilevazione, il valore di <code>size</code> inviato dal mapper
	 * può variare tra un'osservazione e l'altra. Il metodo restituisce la dimensione che ha
	 * la frequenza massima.<br>
	 * Ad esempio se per il determinato bin abbiamo 5 osservazioni del tipo:
	 * <i>id, binId, bike, size<br></i>
	 * 0, 0, 5, 20<br>
	 * 0, 0, 10, 20<br>
	 * 0, 0, 10, 19<br>
	 * 0, 0, 10, 23<br>
	 * 0, 0, 15, 20<br>
	 * la dimensione dello stallo è 20, perché quella che ha massima frequenza.
	 * 
	 * @param values lista che contiene tutte le osservazioni lette dal Mapper {@link BikeMiBinMapper} 
	 * 				per quel determinato bin e per quella determinata stazione.
	 * @return la dimensione del bike station
	 * @see {@link BikeMiBinMapper}
	 */
	private int getBikeStationSize(List<Text> values){
		// Store the frequency of each size read
		HashMap<Integer, Integer> frequency = new HashMap<Integer, Integer>();
		for (Text value : values){
			// Index 0 --> bike 
			// Index 1 --> size
			int size = Integer.parseInt(value.toString().split(BikeMiBinDriver.SPLITTER_BIN)[1]); // Get size
			if(frequency.containsKey(size)) 
				frequency.put(size, frequency.get(size)+1); // Increment frequency
			else 
				frequency.put(size, 1);  
		}

		// Get the size with max frequency from the map
		int correctSize = -1;
		int maxFrequency = Integer.MIN_VALUE;
		for (Entry<Integer, Integer> entry : frequency.entrySet()){
			if (entry.getValue() >= maxFrequency){
				correctSize = entry.getKey();
				maxFrequency = entry.getValue();
			}
		}

		return correctSize;
	}

	/**
	 * Crea una lista dall'Iterable inviato al Reducer.
	 *
	 * @param values tutte le osservazioni lette dal Mapper {@link BikeMiBinMapper} 
	 * 				per quel determinato bin e per quella determinata stazione.
	 * @return lista che contiene tutte le osservazioni lette dal Mapper {@link BikeMiBinMapper} 
	 * 				per quel determinato bin e per quella determinata stazione.
	 * 
	 * @see {@link BikeMiBinMapper}
	 */
	private List<Text> getListFromIterable(Iterable<Text> values){
		List<Text> list = new ArrayList<Text>();
		for (Text value : values) 
			list.add(value);

		return list;

	}

	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) 
			throws IOException, InterruptedException{	

		// get the key
		String[] keyValues = key.toString().split(BikeMiBinDriver.SPLITTER_BIN);

		int id = -1; // Get bike station id
		int binId = -1; // Get the unique bin id
		try{ 
			id = Integer.parseInt(keyValues[0].trim());
			binId = Integer.parseInt(keyValues[1].trim());
		}catch(NumberFormatException e){
			// If exception return Something goes wrong
			return;
		}

		// Get list from iterable
		List<Text> valuesList = getListFromIterable(values);
		// Get the right size
		int correctSize = getBikeStationSize(valuesList);

		// Get the day id
		int dayId =  BinUtil.getDayIdFromUniqueBinIdInAllDays(binsPerDay, binId);
		// Get the daily bin id
		int dailyBinId = BinUtil.getBinIdFromUniqueBinIdInAllDays(binsPerDay, binId); 	

		// Calculate sum and count 
		int size = 0;
		double count = 0;
		double sum = 0;
		for (Text value : valuesList){
			String[] currentValues = value.toString().split(BikeMiBinDriver.SPLITTER_BIN);
			size = Integer.parseInt(currentValues[1].trim());
			if (size == correctSize){
				sum += Integer.parseInt(currentValues[0].trim());
				count++;
			}
		}

		// Calculate average
		double average = (double)sum / (double)count;

		// Calculate the outcome variable
		int percId = -1;
		if (average == correctSize)
			percId = numTarget - 1;
		else
			percId = (int)((average / correctSize) / numClassLabelTreshold);

		// Set the key (dayId)
		wInt.set(dayId);
		// Set the values
		wText.set(id + BikeMiBinDriver.SPLITTER_BIN + dailyBinId + BikeMiBinDriver.SPLITTER_BIN
				+ average + BikeMiBinDriver.SPLITTER_BIN + correctSize + BikeMiBinDriver.SPLITTER_BIN + percId);

		// Write the pair<key, values>
		context.write(wInt, wText);

	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		// Get the configuration of the Job
		Configuration conf = context.getConfiguration();

		// Get values from the configuration class and set the own variable
		numTarget = conf.getInt(BikeMiBinDriver.NUM_TARGET_PROPERTY, 2);
		numClassLabelTreshold = 1D / (double) numTarget;
		long interval = conf.getLong(BikeMiBinDriver.INTERVAL_PROPERTY, 0);
		binsPerDay = (int) (MILLS_IN_A_DAY / interval);

	}
}
