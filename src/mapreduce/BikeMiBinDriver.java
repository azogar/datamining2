package mapreduce;

import java.io.IOException;
import java.util.Date;

import main.BikemiMain;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.mahout.common.HadoopUtil;

/**
 * Driver che si occupa di lanciare il Job MapReduce che permette di trasformare il dataset
 * in un formato adatto al training dei predittori implementati.
 * Ogni transazione del dataset iniziale contiene le seguenti informazione:<br>
 * <li> <i>id</i>: id univoco del bike station {0, ... , NBS-1} dove NBS è il numero totale dei bike station;</li>
 * <li> <i>bike</i>: numero di biciclette disponibili nel bike station;</li>
 * <li> <i>free</i>: numero di stalli liberi nel bike station;</li>
 * <li> <i>timestamp</i>: timestamp della rilevazione.</li>
 * Le infomazione sono separate tra loro con il carattere " " in semplici file di testo.<br>
 * Ogni transazione del dataset risultate contiene le seguenti informazioni:
 * <li> <i>dayId</i>: id univoco del giorno {0,...,N-1} dove N è il numero di giorni considerati;</li>
 * <li> <i>id:</i>: id univoco del bike station {0, ... , NBS-1} dove NBS è il numero totale dei bike station;</li>
 * <li> <i>binId</i>: id giornaliero univoco del bin {0, ... , NB-1} dove è il numero totale di bins in un giorno;</li>
 * <li> <i>average</i>: valore medio delle bicilette disponibili in un determinato bin</li>
 * <li> <i>size</i>: dimensione del bike station;</li>
 * <li> <i>percId</i>: etichetta della variabile da predire. Identificatore dell'intervallo in cui presente la percentuale 
 * 						da predire. {0, ... NT-1} dove NT (NUM_TARGET) è il numero di possibili valori che la variabile da predire 
 * 						può assumere.</li>
 * Il dataset trasformato è memorizzato in <code>SequenceFile</code> all'interno della cartella bin.
 * 
 * @author Armando Segatori
 *
 */
public class BikeMiBinDriver {

	/**
	 * Splitter utilizzato per separare le informazione del dataset trasformato
	 */
	public static final String SPLITTER_BIN = "\t";
	
	/**
	 * Property per memorizzare il pattern di splitter per recuperare le informazioni dal dataset a trasformare
	 */
	protected static final String SPLITTER_PROPERTY = "splitter";
	
	/**
	 * Property per memoriazzare la data di inizio. 
	 * Tutte le osservazioni precedenti la data di inizio non saranno prese in considerazione.
	 */
	protected static final String START_DATE_PROPERTY = "start_date";
	
	/**
	 * Property per memorizzare la data di fine.
	 * Tutte le osservazioni successive alla data di fine non saranno prese in considerazione.
	 */
	protected static final String END_DATE_PROPERTY = "end_date";
	
	/**
	 * Property per memorizzare il valore della dimensione del bin (in millisecondi)
	 */
	protected static final String INTERVAL_PROPERTY = "interval";
	
	/**
	 * Property per memorizzare il numero di etichette che la variabile di uscita può assumere
	 */
	protected static final String NUM_TARGET_PROPERTY = "num_target";

	/**
	 * Directory di Output 
	 */
	private static final String OUTPUT_PATH = BikemiMain.ROOT_PATH + "bin";
	
	/**
	 * Millisecondi in un giorno.
	 */
	private static final long MILLS_IN_A_DAY = 86400000; //  24 (hors) * 60 (minutes) * 60 (seconds) * 1000 (milliseconds);

	/**
	 * Esegue il Job MapReduce. 
	 * Per maggiori dettagli sulla logica del Job leggere la javadoc relativa alla classe.
	 * 
	 * @param splitter pattern che identifica come separare le informazioni di ogni transazione memorizzate nel dataset iniziale
	 * @param startDate data di inizio
	 * @param days numero di giorni consecutivi da considerare
	 * @param interval dimensione del bin in millisecondi
	 * @param numTarget numero di etichette che la variabile di uscita può assumere
	 * @param input directory di input
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 */
	public static void runJob(String splitter, Date startDate, 
			Integer days, Long interval, Integer numTarget, String input) 
				throws IOException, InterruptedException, ClassNotFoundException{

		// Get the end date
		Date endDate = new Date(startDate.getTime() + (days * MILLS_IN_A_DAY));
		
		// Set all the parameter in the Configuration class
		Configuration conf = new Configuration();
		conf.set(SPLITTER_PROPERTY, splitter);
		conf.setLong(START_DATE_PROPERTY, startDate.getTime());
		conf.setLong(END_DATE_PROPERTY, endDate.getTime());
		conf.setLong(INTERVAL_PROPERTY, interval);
		conf.setInt(NUM_TARGET_PROPERTY, numTarget);

		// Create the Job
		Job job = new Job(conf, "Bike Sharing MapReduce Job: " + input);
		job.setJarByClass(BikeMiBinDriver.class);
		
		// Set output for the Job
	    job.setOutputKeyClass(IntWritable.class);
	    job.setOutputValueClass(Text.class);
		
	    // Set the outputs for the Map
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
	    
        // Set the input and output path
	    Path inputPath = new Path(input);

	    FileInputFormat.addInputPath(job, inputPath);
	    Path outPath = new Path(OUTPUT_PATH);
	    FileOutputFormat.setOutputPath(job, outPath);
	    
	    // Delete previous results
	    HadoopUtil.delete(conf, outPath);
	    
	    // Set the input and the output format
	    job.setInputFormatClass(TextInputFormat.class);
//	    job.setOutputFormatClass(TextOutputFormat.class);
	    job.setOutputFormatClass(SequenceFileOutputFormat.class);
	    
	    // Set mapper and reducer class
	    job.setMapperClass(BikeMiBinMapper.class);
	    job.setReducerClass(BikeMiBinReducer.class);

		// Launch the Job
	    boolean succeeded = job.waitForCompletion(true);
	    if (!succeeded) 
	      throw new IllegalStateException("Job failed!");
	    
	}

	/**
	 * Ritorla la directory di output
	 * @return il percorso dove è stato memorizzato il dataset trasformato.
	 */
	public static Path getOutputPath(){
		return new Path(OUTPUT_PATH);
	}

}
