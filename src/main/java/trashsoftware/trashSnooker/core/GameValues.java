package trashsoftware.trashSnooker.core;

public class GameValues {
	
	public static final GameValues SNOOKER_VALUES = new Builder()
			.tableDimension(3820.0, 3569.0, 2035.0, 1788.0)
			.ballSize(52.5)
			.holeSize(85.0, 85.0)
			.build();

	public static final GameValues MINI_SNOOKER_VALUES = new Builder()
			.tableDimension(2830.0, 2540.0, 1550.0, 1270.0)
			.ballSize(52.5)
			.holeSize(85.0, 100.0)
			.build();

	public static final GameValues CHINESE_EIGHT_VALUES = new Builder()
			.tableDimension(2830.0, 2540.0, 1550.0, 1270.0)
			.ballSize(57.15)
			.holeSize(85.0, 100.0)
			.build();

    public double outerWidth;
	public double outerHeight;
	public double innerWidth;
	public double innerHeight;
	public double leftX, rightX, topY, botY, midX, midY;
	public double maxLength;  // 对角线长度

	public double ballDiameter;
	public double ballRadius;
	public double cornerHoleDiameter, cornerHoleRadius;
	public double midHoleDiameter, midHoleRadius;

	public double midArcRadius;

	public double ballWeightRatio;

	public double cornerHoleDt, cornerHoleTan, cornerArcHeight, cornerArcWidth, cornerArcRadius, cornerArcDiameter,
			cornerLineWh;  // 底袋角直线的占地长宽

	public double[] topLeftHoleXY;
	public double[] botLeftHoleXY;
	public double[] topRightHoleXY;
	public double[] botRightHoleXY;
	public double[] topMidHoleXY;
	public double[] botMidHoleXY;

	public double leftCornerHoleAreaRightX;  // 左顶袋右袋角
	public double midHoleAreaLeftX;  // 中袋左袋角
	public double midHoleAreaRightX;  // 中袋右袋角
	public double rightCornerHoleAreaLeftX;  // 右顶袋左袋角
	public double topCornerHoleAreaDownY;  // 上底袋下袋角
	public double botCornerHoleAreaUpY;  // 下底袋上袋角

	// 中袋袋角弧线
	public double[] topMidHoleLeftArcXy;
	public double[] topMidHoleRightArcXy;
	public double[] botMidHoleLeftArcXy;
	public double[] botMidHoleRightArcXy;

	// 底袋袋角弧线
	public double[] topLeftHoleSideArcXy;  // 左上底袋边库袋角
	public double[] topLeftHoleEndArcXy;  // 左上底袋底库袋角
	public double[] botLeftHoleSideArcXy;
	public double[] botLeftHoleEndArcXy;

	public double[] topRightHoleSideArcXy;
	public double[] topRightHoleEndArcXy;
	public double[] botRightHoleSideArcXy;
	public double[] botRightHoleEndArcXy;

	// 底袋袋角直线
	public double[][] topLeftHoleSideLine;
	public double[][] topLeftHoleEndLine;
	public double[][] botLeftHoleSideLine;
	public double[][] botLeftHoleEndLine;

	public double[][] topRightHoleSideLine;
	public double[][] topRightHoleEndLine;
	public double[][] botRightHoleSideLine;
	public double[][] botRightHoleEndLine;

	public double[][] allCornerArcs;

	public double[][][] allCornerLines;

	private void build() {
		topLeftHoleXY = new double[]
				{leftX - cornerHoleDt, topY - cornerHoleDt};
		botLeftHoleXY = new double[]
				{leftX - cornerHoleDt, botY + cornerHoleDt};
		topRightHoleXY = new double[]
				{rightX + cornerHoleDt, topY - cornerHoleDt};
		botRightHoleXY = new double[]
				{rightX + cornerHoleDt, botY + cornerHoleDt};
		topMidHoleXY = new double[]
				{midX, topY - midHoleRadius};
		botMidHoleXY = new double[]
				{midX, botY + midHoleRadius};

		leftCornerHoleAreaRightX = leftX + cornerHoleRadius + cornerArcWidth;  // 左顶袋右袋角
		midHoleAreaLeftX = midX - midHoleDiameter;  // 中袋左袋角
		midHoleAreaRightX = midHoleAreaLeftX + midHoleDiameter * 2;  // 中袋右袋角
		rightCornerHoleAreaLeftX = rightX - cornerHoleRadius - cornerArcWidth;  // 右顶袋左袋角
		topCornerHoleAreaDownY = topY + cornerHoleRadius + cornerArcWidth;  // 上底袋下袋角
		botCornerHoleAreaUpY = botY - cornerHoleRadius - cornerArcWidth;  // 下底袋上袋角

		// 中袋袋角弧线
		topMidHoleLeftArcXy =
				new double[]{topMidHoleXY[0] - midHoleDiameter, topMidHoleXY[1]};
		topMidHoleRightArcXy =
				new double[]{topMidHoleXY[0] + midHoleDiameter, topMidHoleXY[1]};
		botMidHoleLeftArcXy =
				new double[]{botMidHoleXY[0] - midHoleDiameter, botMidHoleXY[1]};
		botMidHoleRightArcXy =
				new double[]{botMidHoleXY[0] + midHoleDiameter, botMidHoleXY[1]};

		// 底袋袋角弧线
		topLeftHoleSideArcXy =  // 左上底袋边库袋角
				new double[]{leftX + cornerHoleRadius + cornerArcWidth, topY - cornerArcRadius};
		topLeftHoleEndArcXy =  // 左上底袋底库袋角
				new double[]{leftX - cornerArcRadius, topY + cornerHoleRadius + cornerArcWidth};
		botLeftHoleSideArcXy =
				new double[]{leftX + cornerHoleRadius + cornerArcWidth, botY + cornerArcRadius};
		botLeftHoleEndArcXy =
				new double[]{leftX - cornerArcRadius, botY - cornerHoleRadius - cornerArcWidth};

		topRightHoleSideArcXy =
				new double[]{rightX - cornerHoleRadius - cornerArcWidth, topY - cornerArcRadius};
		topRightHoleEndArcXy =
				new double[]{rightX + cornerArcRadius, topY + cornerHoleRadius + cornerArcWidth};
		botRightHoleSideArcXy =
				new double[]{rightX - cornerHoleRadius - cornerArcWidth, botY + cornerArcRadius};
		botRightHoleEndArcXy =
				new double[]{rightX + cornerArcRadius, botY - cornerHoleRadius - cornerArcWidth};

		// 底袋袋角直线
		topLeftHoleSideLine =
				new double[][]{{leftX, topY - cornerHoleTan}, {leftX + cornerLineWh, topY - cornerArcHeight}};
		topLeftHoleEndLine =
				new double[][]{{leftX - cornerHoleTan, topY}, {leftX - cornerArcHeight, topY + cornerLineWh}};
		botLeftHoleSideLine =
				new double[][]{{leftX, botY + cornerHoleTan}, {leftX + cornerLineWh, botY + cornerArcHeight}};
		botLeftHoleEndLine =
				new double[][]{{leftX - cornerHoleTan, botY}, {leftX - cornerArcHeight, botY - cornerLineWh}};

		topRightHoleSideLine =
				new double[][]{{rightX, topY - cornerHoleTan}, {rightX - cornerLineWh, topY - cornerArcHeight}};
		topRightHoleEndLine =
				new double[][]{{rightX + cornerHoleTan, topY}, {rightX + cornerArcHeight, topY + cornerLineWh}};
		botRightHoleSideLine =
				new double[][]{{rightX, botY + cornerHoleTan}, {rightX - cornerLineWh, botY + cornerArcHeight}};
		botRightHoleEndLine =
				new double[][]{{rightX + cornerHoleTan, botY}, {rightX + cornerArcHeight, botY - cornerLineWh}};

		allCornerArcs = new double[][] {
				topLeftHoleSideArcXy,
				topLeftHoleEndArcXy,
				topRightHoleSideArcXy,
				topRightHoleEndArcXy,
				botLeftHoleSideArcXy,
				botLeftHoleEndArcXy,
				botRightHoleSideArcXy,
				botRightHoleEndArcXy
		};

		allCornerLines = new double[][][] {
				topLeftHoleSideLine,  // "\"
				topLeftHoleEndLine,  // "\"
				botRightHoleSideLine,  // "\"
				botRightHoleEndLine,  // "\"
				topRightHoleSideLine,  // "/"
				topRightHoleEndLine,  // "/"
				botLeftHoleSideLine,  // "/"
				botLeftHoleEndLine  // "/"
		};
	}

    public static class Builder {
		private final GameValues values = new GameValues();
		
		public Builder tableDimension(double outerWidth, double innerWidth, double outerHeight, double innerHeight) {
			values.outerWidth = outerWidth;
			values.innerWidth = innerWidth;
			values.outerHeight = outerHeight;
			values.innerHeight = innerHeight;
			values.leftX = (outerWidth - innerWidth) / 2;
			values.rightX = innerWidth + values.leftX;
			values.topY = (outerHeight - innerHeight) / 2;
			values.botY = innerHeight + values.topY;
			values.midX = outerWidth / 2;
			values.midY = outerHeight / 2;
			values.maxLength = Math.hypot(outerHeight, outerWidth);
			return this;
		}
		
		public Builder ballSize(double diameter) {
			values.ballDiameter = diameter;
			values.ballRadius = diameter / 2;
			values.ballWeightRatio = Math.pow(diameter, 3) / Math.pow(52.5, 3);
			return this;
		}
		
		public Builder holeSize(double cornerHoleDiameter, double midHoleDiameter) {
			values.cornerHoleDiameter = cornerHoleDiameter;
			values.midHoleDiameter = midHoleDiameter;
			values.cornerHoleRadius = cornerHoleDiameter / 2;
			values.midHoleRadius = midHoleDiameter / 2;
			values.midArcRadius = values.midHoleRadius;

			values.cornerHoleDt = values.cornerHoleRadius / Math.sqrt(2);
			values.cornerHoleTan = values.cornerHoleRadius * Math.sqrt(2);
			values.cornerArcHeight = values.cornerHoleTan - values.cornerHoleRadius;
			values.cornerArcWidth = values.cornerArcHeight * Math.tan(Math.toRadians(67.5));
			values.cornerArcRadius = values.cornerArcWidth + values.cornerArcHeight;
			values.cornerArcDiameter = values.cornerArcRadius * 2;
			values.cornerLineWh = values.cornerHoleRadius;  // 底袋角直线的占地长宽
			return this;
		}
		
		public GameValues build() {
			values.build();
			return values;
		}
    }
}
