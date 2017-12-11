
public class SlopeIntercept {
	
	private double m, b;
	
	public SlopeIntercept(){
		m = 0;
		b = 0;
	}
	
	public SlopeIntercept(double m, double b){
		this.m = m;
		this.b = b;
	}

	public double getM() {
		return m;
	}

	public void setM(double m) {
		this.m = m;
	}

	public double getB() {
		return b;
	}

	public void setB(double b) {
		this.b = b;
	}
	
	public double compute(int x){
		return x * m + b;
	}

}
