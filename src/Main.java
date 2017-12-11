import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

import com.jaunt.NotFound;
import com.jaunt.ResponseException;
import com.jaunt.UserAgent;
import com.jaunt.component.Table;

/**
 * 
 * Program that uses linear regression to estimate the price of a given
 * Stock in the future.  Program requires three arguments:
 * Ticker - The name of the Stock
 * Time - How many days into the future that you want to predict
 * Method - 0 For online, 1 For offline; To use offline you must download the csv file 
 * from https://finance.yahoo.com and move it into the project directory
 * 
 * @author Cole Brower, Jacob Downey
 *
 */
public class Main {
	
	public static final String TAG = "Close";
	
	/**
	 * Computes the function of the line of best fit for the given set of points
	 * Formula for computing line of best fit from:
	 * https://www.varsitytutors.com/hotmath/hotmath_help/topics/line-of-best-fit
	 * 
	 * 0(n^2)
	 * @param pts X, Y Points
	 * @return A SlopeIntercept object representing a linear function of the line of best fit
	 */
	public static SlopeIntercept computeFunction(LinkedList<Point2D.Double> pts){
		double xAve = 0;
		double yAve = 0;
		double num = 0;
		double den = 0;
		int len = pts.size();
		for (Point2D.Double pt: pts){
			xAve += pt.x/len;
			yAve += pt.y/len;
		}
		//Compute slope
		for (Point2D.Double pt: pts){
			num += (pt.x - xAve) * (pt.y - yAve);
			den += Math.pow(pt.x - xAve, 2);
		}
		
		double m = num/den;
		System.out.println(m);
		double b = yAve - m * xAve;
		System.out.println(b);
		return new SlopeIntercept(m, b);
	}
	
	/**
	 * Scrapes the table containing the closing stock prices of:
	 * "https://finance.yahoo.com/quote/" + tick + "/history?&interval=1d&filter=history&frequency=1d"
	 * 
	 * O(n^2)
	 * 
	 * @param tick The Ticker of the Stock that is being scraped
	 * @return A LinkedList<Point2D.Double> containing points for a graph.  X is time starting at 0, Y is stock price
	 * @throws ResponseException
	 * @throws NotFound
	 */
	public static LinkedList<Point2D.Double> loadFromScrape(String tick) throws ResponseException, NotFound{
		LinkedList<Point2D.Double> pts = new LinkedList<>();
		int closeIndex = 4; //In the table, the value of the stock after closing is the fifth column
		UserAgent agent = new UserAgent();
		agent.visit("https://finance.yahoo.com/quote/" + tick + "/history?&interval=1d&filter=history&frequency=1d");
		Table t = agent.doc.getTable(0);
		
		/*
		 * Starts at index 1 to skip table headings
		 * Iterates to Integer.MAX_VALUE because the Table Object iterator does not function and
		 * the Table Object contains no methods for getting the number of rows or columns
		 * 
		 * We iterate through the data until we reach the text at the bottom of the table and
		 * trigger a NumberFormatException which tells us to stop filling the LinkedList
		 */
		for (int i = 1; i < Integer.MAX_VALUE; i++){
			try {
				double data = Double.parseDouble(t.getCell(closeIndex, i).getElement(0).getText().replaceAll(",", ""));
				pts.add(new Point2D.Double(i-1, data));
			} catch (NumberFormatException ex){
				break;
			}
		}
		
		int len = pts.size() - 1;
		
		/*
		 * The html table starts with the most recent price and descends in time
		 * We switch the x coordinates so that we can predict the future.
		 * 
		 * We can't read the table backwards or get the size early because the Table
		 * object does not include that functionality thus requireing another n iterations
		 */
		for (Point2D.Double pt : pts){
			pt.x = len - pt.x;
		}
		
		return pts;
	}
	
	/**
	 * Retrieves the closing costs of stock from a CSV file downloaded from:
	 * "https://finance.yahoo.com/quote/" + tick + "/history?&interval=1d&filter=history&frequency=1d"
	 * 
	 * O(n)
	 * @param filename CSV filename
	 * @return A LinkedList<Point2D.Double> containing points for a graph.  X is time starting at 0, Y is stock price
	 */
	public static LinkedList<Point2D.Double> loadFromCSV(String filename){
		File f = null;
		Scanner scan = null;
		LinkedList<Point2D.Double> pts = new LinkedList<>();
		try{
			//Finding the index of the closing stock price in the CSV file
			f = new File(filename);
			scan = new Scanner(f);
			String ln = scan.nextLine();
			String[] columns = ln.split(",");
			int counter = 0;
			int index = -1;
			for (String s : columns){
				if (s.equals(TAG)){
					index = counter;
					break;
				}
				counter++;
			}
			//Gathering data for the LinkedList<Point2D.Double>
			counter = 0;
			while (scan.hasNextLine()){
				columns = scan.nextLine().split(",");
				pts.add(new Point2D.Double(counter++, Double.parseDouble(columns[index])));
			}
			return pts;
		} catch (IOException e){
			System.err.println("Failed to Load File");
			return null;
		} finally {
			if (scan != null){
				scan.close();
			}
		}
	}

	public static void main(String[] args) {
		LinkedList<Point2D.Double> pts = null;
		if (args.length < 3){
			System.err.println("Usage: java Main <Stock Ticker> <Time in days> <0=Online, 1=Read From CSV>");
			System.exit(1);
		}
		
		if (Integer.parseInt(args[2]) == 0){
			try {
				pts = loadFromScrape(args[0]);
			} catch (NotFound | ResponseException e) {
				e.printStackTrace();
			}
		} else {
			pts = loadFromCSV(args[0] + ".csv");
		}
		int time = Integer.parseInt(args[1]);
		SlopeIntercept mb = computeFunction(pts);
		System.out.printf("The price " + time + " days from now should be about: $%.2f", mb.compute(time + pts.size() - 1));
	}
}