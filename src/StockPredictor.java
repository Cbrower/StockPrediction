import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

/*
 * Jaunt is downloadable from http://jaunt-api.com/
 */
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
public class StockPredictor {
	
	public static final String TAG = "Close";
	
	private LinkedList<Point2D.Double> pts;
	private String tick;
	private SlopeIntercept line = null;
	
	public StockPredictor(String ticker){
		tick = ticker;
	}
	
	/**
	 * Computes the function of the line of best fit for the given set of points
	 * Formula for computing line of best fit from:
	 * https://www.varsitytutors.com/hotmath/hotmath_help/topics/line-of-best-fit
	 * 
	 * 0(n)
	 */
	private void computeFunction(){
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
		line =  new SlopeIntercept(m, b);
	}
	
	/**
	 * Scrapes the table containing the closing stock prices of:
	 * "https://finance.yahoo.com/quote/" + tick + "/history?&interval=1d&filter=history&frequency=1d"
	 * 
	 * O(n)
	 * 
	 * @param tick The Ticker of the Stock that is being scraped
	 * @return A LinkedList<Point2D.Double> containing points for a graph.  X is time starting at 0, Y is stock price
	 * @throws ResponseException
	 * @throws NotFound
	 */
	public void loadFromScrape() throws ResponseException, NotFound{
		pts = new LinkedList<>();
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
		
		try {
			agent.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		computeFunction();
	}
	
	/**
	 * Retrieves the closing costs of stock from a CSV file downloaded from:
	 * "https://finance.yahoo.com/quote/" + tick + "/history?&interval=1d&filter=history&frequency=1d"
	 * 
	 * O(n)
	 */
	public void loadFromCSV(){
		String filename = tick + ".csv";
		File f = null;
		Scanner scan = null;
		pts = new LinkedList<>();
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
			computeFunction();
		} catch (IOException e){
			System.err.println("Failed to Load File");
		} finally {
			if (scan != null){
				scan.close();
			}
		}
	}
	
	/**
	 * Computes the function of the line of best fit for the stock
	 * Precondition: Must have already loaded from scrape or from csv before
	 * calling this function
	 * 
	 * @param x The number of days into the future that you want to compute
	 * @return The predicted stock price in the future as a double
	 */
	public double predict(int x){
		return line.compute(x + pts.size()-1);
	}

	public static void main(String[] args) {
		StockPredictor stock = null;
		if (args.length < 3){
			System.err.println("Usage: java Main <Stock Ticker> <Time in days> <0=Online, 1=Read From CSV>");
			System.exit(1);
		}
		stock = new StockPredictor(args[0]);
		if (Integer.parseInt(args[2]) == 0){
			try {
				stock.loadFromScrape();
			} catch (NotFound | ResponseException e) {
				e.printStackTrace();
			}
		} else {
			stock.loadFromCSV();
		}
		int time = Integer.parseInt(args[1]);
		System.out.printf("The price " + time + " days from now should be about: $%.2f", stock.predict(time));
	}
	
	/*
	 * Kept this as a separate file but due to the requirements of one file upload for
	 * turnitin, I moved this into the same file
	 */
	/**
	 * Represents a function y = mx + b
	 * 
	 * @author Cole Brower and Jacob Downey
	 *
	 */
	protected class SlopeIntercept {
		
		private double m, b;
		
		/**
		 * Assigns default values of zero for the slope and the y-intercept
		 */
		public SlopeIntercept(){
			m = 0;
			b = 0;
		}
		
		/**
		 * Creates a SlopeIntercept Object
		 * @param m The slope of the line
		 * @param b The y-intercept of the line
		 */
		public SlopeIntercept(double m, double b){
			this.m = m;
			this.b = b;
		}

		/**
		 * Returns the slope of the line
		 * @return A double representing the slope
		 */
		public double getM() {
			return m;
		}

		/**
		 * Changes the value of the slope of the line
		 * @param m A double representing the new slope of the line
		 */
		public void setM(double m) {
			this.m = m;
		}

		/**
		 * Returns the y-intercept of the line
		 * @return A double representing the y-intercept
		 */
		public double getB() {
			return b;
		}

		/**
		 * Changes the value of the y-intercept of the line
		 * @param b A double representing the y-intercept of the line
		 */
		public void setB(double b) {
			this.b = b;
		}
		
		/**
		 * Computes the f(x) value of the line f(x) = mx + b
		 * @param x The x value used in f(x)
		 * @return A double value representing the value returned by mx + b
		 */
		public double compute(int x){
			return x * m + b;
		}

	}
}