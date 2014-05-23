package utility;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.DatatypeConverter;

/**
 * Classe di utilità per convertira le date in stringhe e le stringhe in date.
 * 
 * @author Armando Segatori
 *
 */
public class DateUtil {

	/**
	 * Trasforma un oggetto Date in stringa.
	 * 
	 * @param date la data da trasformare in stringa
	 * @returnla una stringa che rappresenta la data nel formato YYYY-MM-GGTHH:MM:SS+TIMEZONE
	 */
	public static String dateToIso8601String(Date date){
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(date);
		return DatatypeConverter.printDateTime(calendar);
	}
	
	/**
	 * Trasforma la stringa in oggetto Date
	 * 
	 * @param dateString  stringa che rappresenta la data nel formato YYYY-MM-GGTHH:MM:SS+TIMEZONE
	 * @return oggetto Date
	 */
	public static Date stringIso8601ToDate(String dateString){
		return ((Calendar)DatatypeConverter.parseDateTime(dateString)).getTime();
	}
	
}
