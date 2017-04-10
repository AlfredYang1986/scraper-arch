package excel.data;

/**
 * Created by Alfred on 10/04/2017.
 */
public class dianping_select_row {
	private String shop;
	private String course;
	private String other;

	public String getShop() {
		return shop;
	}

	public void setShop(String s) {
		this.shop = s;
	}

	public String getCourse() {
		return course;
	}

	public void setCourse(String s) {
		this.course = s;
	}

	public String getOther() {
		return other;
	}

	public void setOther(String s) {
		this.other = s;
	}

	@Override
	public String toString() {
		return "shop: " + this.shop + "\ncourse: " + this.course + "\nother: " + this.other;
	}
}
