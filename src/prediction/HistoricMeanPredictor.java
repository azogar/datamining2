package prediction;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.mahout.math.Vector;

public class HistoricMeanPredictor extends AbstractPredictor {


	private static final String NAME = "Historic Mean Value Regressor";
	
	// TODO Add here some variables for the project

	public HistoricMeanPredictor(){
		super();
		// TODO Initialize here the class variables 
	}
	
	@Override
	public int classify(Vector vector) {
		// TODO Implement me for the project
		return 0;
	}
	
	@Override
	public void buildClassifier(Path input, String pattern, int days, int binsPerDay, int numTarget) 
			throws IOException {
		// TODO Implement me for the project
	}
	
	@Override
	public String printInfo() {
		StringBuilder sb = new StringBuilder(NAME + ":\n");
		sb.append(super.printInfo());
		return sb.toString();
	}	
}
