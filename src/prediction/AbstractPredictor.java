package prediction;

import org.apache.mahout.classifier.ConfusionMatrix;

/**
 * Classe astratta che implementa i metodi comuni tra tutti i predittori
 * 
 * @author Armando Segatori
 *
 */
public abstract class AbstractPredictor implements Predictor{

	/**
	 * Numero di valori che la variabile di uscita può assumere
	 */
	protected int numCategories;
	
	/**
	 * Finestra di predizione che indica di quanti bin in avanti vogliamo spostarci
	 * per la predizione
	 */
	protected int pw;
	
	/**
	 *  Matrice di confusione
	 */
	protected ConfusionMatrix cm;

	/**
	 * Costruttore di default
	 */
	public AbstractPredictor(){
		this.numCategories = 0;
		this.pw = 0;
		cm = null;
	}
	
	/**
	 * Costruttore 
	 * 
	 * @param numCategories numero di valori che la variabile di uscita può assumere
	 * @param pw finestra di predizione
	 */
	public AbstractPredictor(int numCategories, int pw){
		this.numCategories = numCategories;
		this.pw = pw;
		cm = null;
	}

	@Override
	public final ConfusionMatrix getConfusionMatrix() {
		return cm;
	}
	
	@Override
	public final int getNumCategories() {
		return numCategories;
	}
	
	@Override
	public final int getPw() {
		return pw;
	}
	
	@Override
	public String printInfo() {
		StringBuilder sb = new StringBuilder();
		if (cm != null){
			sb.append(cm.toString());
			sb.append("Accurancy: " + cm.getAccuracy());
		}
		else
			sb.append("Cannot print the confusion matrix info\n");
		
		return sb.toString();
	}	

}
