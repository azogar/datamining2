package prediction;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.mahout.classifier.ConfusionMatrix;
import org.apache.mahout.math.Vector;

/**
 * Interfaccia che definisce il comportamento dei predittori da implementare
 * 
 * @author Armando Segatori
 *
 */
public interface Predictor {
	
	/**
	 * Costruisce il modello del predittore. Dal file in input utilizza le osservazioni dell'ultimo giorno 
	 * per testare il modello e tutte le altre per allenarlo.
	 * 
	 * @param input directory di input.
	 * @param pattern da utilizzare per recuperare le informazioni dal dataset trasformato.
	 * @param days numero di giorni da considerare
	 * @param binsPerDay numero di bins in un giorno
	 * @param numTarget numero dei possibili valori che la varibaile da predire può assumero
	 * @throws IOException
	 */
	public void buildClassifier(Path input, String pattern, int days, int binsPerDay, int numTarget) throws IOException;
	
	/**
	 * Classificauna determinato vettore.
	 * @param vector vettore che contiene la transazione da classificare
	 * @return l'id del valore che la variabile da predire può assumere
	 */
	public int classify(Vector vector);
	
	/**
	 * Recupera la matrice di confusione
	 * 
	 * @return la matrice di confusion
	 */
	public ConfusionMatrix getConfusionMatrix();
	
	/**
	 * Recupera il numero di valori che la variabile da predire può assumere.
	 *
	 * @return il numero di valori che la variabile da predire può assumere.
	 */
	public int getNumCategories();
	
	/**
	 * Recupera la finestra di predizione utilizzata per predire (PW).
	 * Indica quanto nel futuro vogliamo andare per la nostra predizione
	 * 
	 * @return finestra di predizione utilizzata per predire (PW). Il numero è un intero
	 * 			che specifica di quanti bin vogliamo spostarci nel futuro.
	 */
	public int getPw();
	
	/**
	 * Recupera le informazioni sulle prestazioni del predittore.
	 * 
	 * @return  stringa con tutte le informazioni sulle prestazioni del predittore. 
	 */
	public String printInfo();
}
