package edu.fordham.cis.wisdm.actipebble;

import edu.fordham.cis.wisdm.actipebble.GyroscopeRecord;

public class GyroTest {

	public static void main(String[] args) {
		GyroscopeRecord rec = new GyroscopeRecord(1,2,3,15L);
		System.out.println("Record is " + rec.toString());
		System.out.println("X: " +rec.getX());
	}
}

