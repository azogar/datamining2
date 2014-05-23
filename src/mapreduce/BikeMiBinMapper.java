package mapreduce;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import utility.DateUtil;

/**
 * Il Mapper invia al Reducer le informazioni relative alla disponibilità delle bicilette e la dimensione
 * di una bike station per ogni bin.<br>
 * <li><i>key</i>: stringa in cui è memorizzato l'id del bike station e l'id del bin;
 * <li><i>value:</i>: stringa in cui è memorizzato il numero di biciclette disponibili e la dimensione del bike station. 
 * 
 * @author Armando Segatori
 *
 */
public class BikeMiBinMapper extends Mapper<LongWritable, Text, Text, Text>{

	private static final String DEFAULT_SPLITTER = " ";
	
	/**
	 * Pattern da utilizzare per recuperare le informazioni dal dataset.
	 */
	private Pattern splitter;
	
	/**
	 * Data di inizia (in millisecondi).
	 */
	private long startDateMill;
	
	/**
	 * Data di fine (in millisecondi).
	 */
	private long endDateMill;
	
	/**
	 * Dimensione del bin (in millisecondi).
	 */
	private long interval;

	Text wTextKey = new Text();
	Text wTextValue = new Text();

	
	/**
	 * Controlla che i valore di <i>bike</i> e <i>free</i> della specifica transazione siano validi.
	 * 
	 * @param bike numero di bicilette disponibili. Colonna <i>bike</i> del dataset.
	 * @param free numero di stalli liberi. Colonna <i>free</i> del dataset.
	 * @return <code>true</code> if values are valid, <code>false</code> otherwise.
	 */
	private boolean isValidObservation(int bike, int free){
		int size = bike + free;
		return (size > 0 && bike >= 0 && free >= 0);
	}
	
	/**
	 * Controlla che la data letta nella transazione sia valida, ossia
	 * che si trovi all'interno della finestra temporale specificata da <code>start date</code>
	 * e <code> end date</code>.
	 * 
	 * @param millDate la data da controllare in millisecondi
	 * @return <code>true</code> if the date is valid, <code>false</code> otherwise.
	 */
	private boolean isValidTimestamp(long millDate){
		return (millDate >= startDateMill && millDate < endDateMill);
	}
	
	@Override
	protected void map(LongWritable key, Text value, Context context) 
			throws IOException, InterruptedException{

		String[] values = splitter.split(value.toString());
		int id = -1;
		int bike = -1;
		int free = -1;

		try{ 
			// Parse all data
			id 	 = Integer.parseInt(values[0].trim());
			bike = Integer.parseInt(values[1].trim());
			free = Integer.parseInt(values[2].trim());
		}catch(NumberFormatException e){
			// If exception then return. 
			// The transaction is not taken into account
			return;
		}

		// Get the timestamp (in millisecond)
		long millDate = DateUtil.stringIso8601ToDate(values[3].trim()).getTime();
		
		// Check date and the other info
		if (!isValidTimestamp(millDate) || !isValidObservation(bike, free))
			return;

		// Normalize the start date
		millDate -= startDateMill;
		
		// Calculate the station size
		int size = bike + free;

		// Calculate the bin id
		int binId = (int) (millDate / interval);

		// Set the key
		wTextKey.set(id + BikeMiBinDriver.SPLITTER_BIN + binId);
		// Set the values
		wTextValue.set(bike + BikeMiBinDriver.SPLITTER_BIN + size);

		// Write the pair<key, value>
		context.write(wTextKey, wTextValue);	

	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		// Get the configuration of the Job
		Configuration conf = context.getConfiguration();

		// Get values from the configuration class and set the own variable
		String pattern = conf.get(BikeMiBinDriver.SPLITTER_PROPERTY, DEFAULT_SPLITTER);
		splitter = Pattern.compile(pattern);
		startDateMill = conf.getLong(BikeMiBinDriver.START_DATE_PROPERTY, 0);
		endDateMill = conf.getLong(BikeMiBinDriver.END_DATE_PROPERTY, 0);
		interval = conf.getLong(BikeMiBinDriver.INTERVAL_PROPERTY, 1);

	}

}
