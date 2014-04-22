package gov.usgs.vdx.data.wave;

public enum LengthFlag {
	   FOUR(4),
	   EIGHT(8);
	   private int length;
	   private LengthFlag(int length) {
	      this.length = length;
	   }
	   public int getLength() {
	      return length;
	   }
	}