package utility;

/** 
 * Classe di utilità per gestire i bin.
 * In particolare un bin è identificato univocamente tramite il suo id: un intero
 * crescente da 0 a TB-1 dove TB è il numero di bins all'interno della finestra temporale 
 * in esame. <br>Ad esempio, se ogni bin ha dimensione 15 minuti e consideriamo una finestra temporale
 * di 4 giorni allora ogni bin può essere identificato tramite un intero che va da 0 a 383.
 * Il bin con id 96 corrisponde al primo bin del secondo giorno, il bin con id 97 corrisponde 
 * al secondo bin del secondo giorno e cos' via. Conoscendo quindi la dimensione del bin e il suo identificatore
 * è possibile recuperare sia l'i-esimo giorno a cui appartiente, sia l'id del bin ristretto all'i-esimo
 * giorno.
 * 
 * @author Armando Segatori
 *
 */
public class BinUtil {

	/**
	 * Calcola l'id univoco all'interno della finestra temporale in considerazione conoscendo
	 * l'id del giorno, il numero di bins in un giorno e l'id giornaliero del bin. Per id giornaliero del bin 
	 * si intende l'id del bin con una finestra temporale di un giorno.<br>
	 * Il metodo calcola l'id univoco del bin esteso alla finestra temporale come segue <code>dayId * binsPerDay + dailyBinId</code><br>
	 * Ad esempio se ogni bin ha dimensione 15 minuti e vogliamo conoscere l'id univoco del quarto bin 
	 * del terzo giorno, il calcolo sarà 2 * 96 * + 3 =  195<br>
	 * dove:
	 * <li> 2 è l'id del giorno 3;</li>
	 * <li> 96 è il numero di bins in un giorno: i minuti totali in un giorno (1440) diviso la dimensione 
	 * 		di un bin (15 minuti);</li>
	 * <li> 3 è l'id giornaliero del quarto bin.</li>
	 * 
	 * @param dayId id del giorno che vogliamo considerare (intero da 0 a D-1 dove D è il numero di giorni della
	 * 			finestra temporale)
	 * @param binsPerDay numero di bins in un giorno
	 * @param dailyBinId id univoco del bin giornaliero (ossia quando la finestra temporale coincide con un giorno)
	 * @return l'id univoco del bin dell'i-esimo giorno
	 */
	public static int getUniqueBinIdInAllDays(int dayId, int binsPerDay, int dailyBinId){
		return (dayId * binsPerDay) + dailyBinId;
	}
	
	/**
	 * Calcola l'id univoco giornaliero del bin (ossia l'id del bin con finestra temporale di un singolo giorno) conoscendo 
	 * il numero di bins in un giorno e l'id univoco del bin nella finestra temporale in considerazione. <br>
	 * Il metodo calcola l'id univoco del bin esteso alla finestra temporale come il resto del rapporto
	 * tra <code>uniqueBinId</code> e <code>binsPerDay</code>.
	 * Ad esempio se ogni bin ha dimensione 15 minuti e vogliamo recuperare l'id giornaliero del bin conoscendo il suo id 
	 * univoco (195) all'interno di una finestra temporale più estesa del singolo giorno, il calcolo sarà 195 / 96 =  2 resto 3.<br>
	 * dove:
	 * <li> 195 è l'id univoco del giorno in tutta la finestra temporale in considerazione;</li>
	 * <li> 96 è il numero di bins in un giorno: i minuti totali in un giorno (1440) diviso la dimensione 
	 * 		di un bin (15 minuti)</li>
	 * Il metodo restiuirà il valore 3, ciò significa che il nostro bin (con id 3) rappresenta il quarto bin del giorno.
	 * 
	 * @param binsPerDay numero di bins in un giorno
	 * @param uniqueBinId id univoco del bin in tutta la finestra temporale
	 * @return id giornaliero del bin
	 */
	public static int getBinIdFromUniqueBinIdInAllDays(int binsPerDay, int uniqueBinId){
		return (int) uniqueBinId % binsPerDay; // Esegue la divisione e prende il resto
	}
	
	
	/**
	 * Calcola l'id univoco del giorno conoscendo il numero di bins in un giorno e l'id univoco del bin 
	 * nella finestra temporale in considerazione.<br>
	 * Il metodo calcola l'id del giorno come rapporto tra <code>uniqueBinId</code> e <code>binsPerDay</code>.
	 * Ad esempio se ogni bin ha dimensione 15 minuti e vogliamo recuperare l'id giornaliero del bin conoscendo il suo id 
	 * univoco (195) all'interno di una finestra temporale più estesa del singolo giorno, il calcolo sarà 195 / 96 =  2 resto 3.<br>
	 * dove:
	 * <li> 195 è l'id univoco del giorno in tutta la finestra temporale in considerazione;</li>
	 * <li> 96 è il numero di bins in un giorno: i minuti totali in un giorno (1440) diviso la dimensione 
	 * 		di un bin (15 minuti)</li>
	 * Il metodo restiuirà il valore 2, ciò significa che il nostro giorno (con id 2) rappresenta il terzo giorno.
	 * 
	 * @param binsPerDay numero di bins in un giorno
	 * @param uniqueBinId id univoco del bin in tutta la finestra temporale
	 * @return id del giorno in cui si trova il bin identificato tramite il suo <code>uniqueBinId</code>
	 */
	public static int getDayIdFromUniqueBinIdInAllDays(int binsPerDay, int uniqueBinId){
		return (int) uniqueBinId / binsPerDay; // Esegue la divisione e prende solamente la parte intera
	}
}
