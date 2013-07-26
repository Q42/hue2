package nl.q42.hue2;

import java.util.ArrayList;
import java.util.UUID;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.util.FloatMath;

/**
 * This class comes from the original Android app.
 */
public class PHUtilitiesImpl {
	public static final int CPT_RED=0;    
	public static final int CPT_GREEN=1;    
	public static final int CPT_BLUE=2;
	/*//The colour gamut points for our lamps.
	private static PointF Red = new PointF(0.675F, 0.322F);
	private static PointF Lime = new PointF(0.4091F, 0.518F);
	private static PointF Blue = new PointF(0.167F, 0.04F);*/

	/**
	 * get a unique hash of the device.
	 * 
	 * @return a unique hash of the device.
	 */
	public static String getDeviceId(Context ctx) {
		if (ctx == null) {
			return null;
		}

		String androidId = "" + android.provider.Settings.Secure.getString(ctx.getApplicationContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		String deviceName = getDeviceName();

		UUID deviceUuid = new UUID(androidId.hashCode(), (long)deviceName.hashCode() << 32 | deviceName.hashCode());
		String deviceId = deviceUuid.toString().replace("-", "");

		return deviceId;
	}
	/**
	 * Retrieves the information(deviceId, sim serial number etc) of the device on which app is running 
	 * 
	 * @return the deviceID, if no deviceID is available the Android build MODEL is returned.
	 */
	public static String getDeviceName() {
		final String deviceName;

		String manufacturer = "" + Build.MANUFACTURER;
		String model = Build.MODEL;

		if (model.startsWith(manufacturer)) {
			deviceName = model;
		}
		else {
			deviceName = manufacturer + " " + model;
		}

		return deviceName;
	}

	/**
	 * Generates the color for the given XY values, brightness and light model Id. 
	 * Note: When the exact values cannot be represented, it will return the closest match.
	 * @param xy the float array contain x and the y value.
	 * @param brightness the brightness for the color (between 0.0 - 1.0)
	 * @param model the model of the lamp, example: "LCT001" for hue bulb. Used to calculate the color gamut. 
	 * 			If this value is empty the default gamut values are used.
	 * @return int the Android Color value. If xy is null OR xy is not an array of size 2, Color.BLACK will be returned
	 */
	public static int colorFromXY(float [] points, String model ){    
		PointF xy=new PointF(points[0],points[1]);
		ArrayList<PointF> colorPoints = colorPointsForModel(model);    
		boolean inReachOfLamps =checkPointInLampsReach(xy,colorPoints);        
		if (!inReachOfLamps) {        
			//It seems the colour is out of reach        
			//let's find the closest colour we can produce with our lamp and send this XY value out.                
			//Find the closest point on each line in the triangle.        
			PointF pAB =getClosestPointToPoints(colorPoints.get(CPT_RED), colorPoints.get(CPT_GREEN), xy);  
			PointF pAC =getClosestPointToPoints(colorPoints.get(CPT_BLUE), colorPoints.get(CPT_RED), xy);
			//Point pAC = getClosestPointToPoints:[[colorPoints objectAtIndex:cptBLUE] CGPointValue] point2:[[colorPoints objectAtIndex:cptRED] CGPointValue] point3:xy];       
			PointF pBC =getClosestPointToPoints(colorPoints.get(CPT_GREEN), colorPoints.get(CPT_BLUE), xy);
			//CGPoint pBC = [self getClosestPointToPoints:[[colorPoints objectAtIndex:cptGREEN] CGPointValue] point2:[[colorPoints objectAtIndex:cptBLUE] CGPointValue] point3:xy];               
			//Get the distances per point and see which point is closer to our Point.       
			float dAB = getDistanceBetweenTwoPoints(xy ,pAB);    ;        
			float dAC = getDistanceBetweenTwoPoints(xy ,pAC);        
			float dBC = getDistanceBetweenTwoPoints(xy ,pBC);        
			float lowest = dAB;        
			PointF closestPoint = pAB;                
			if (dAC < lowest) {            
				lowest = dAC;            
				closestPoint = pAC;        
			}        
			if (dBC < lowest) {            
				lowest = dBC;            
				closestPoint = pBC;        
			}                
			//Change the xy value to a value which is within the reach of the lamp.        
			xy.x = closestPoint.x;        
			xy.y = closestPoint.y;    
		}        
		float x = xy.x;    
		float y = xy.y;    
		float z = 1.0f - x - y;    
		float Y = 1.0f;    
		float X = (Y / y) * x;    
		float Z = (Y / y) * z;       
		/*// Wide gamut conversion    
		float r = X  * 1.612f - Y * 0.203f - Z * 0.302f;    
		float g = -X * 0.509f + Y * 1.412f + Z * 0.066f;    
		float b = X  * 0.026f - Y * 0.072f + Z * 0.962f;      */  
		// sRGB conversion
		//    float r = X  * 3.2410f - Y * 1.5374f - Z * 0.4986f;
		//float g = -X * 0.9692f + Y * 1.8760f + Z * 0.0416f;
		//    float b = X  * 0.0556f - Y * 0.2040f + Z * 1.0570f;   

		// sRGB D65 conversion
		float r = X  * 3.2406f - Y * 1.5372f - Z * 0.4986f;
		float g = -X * 0.9689f + Y * 1.8758f + Z * 0.0415f;
		float b = X  * 0.0557f - Y * 0.2040f + Z * 1.0570f;
		
		if (r > b && r > g && r > 1.0f) {
			// red is too big
			g = g / r;
			b = b / r;
			r = 1.0f;
		}
		else if (g > b && g > r && g > 1.0f) {
			// green is too big
			r = r / g;
			b = b / g;
			g = 1.0f;
		}
		else if (b > r && b > g && b > 1.0f) {
			// blue is too big
			r = r / b;
			g = g / b;
			b = 1.0f;
		}
		// Apply gamma correction
		r = r <= 0.0031308f ? 12.92f * r : (1.0f + 0.055f) * (float)Math.pow(r, (1.0f / 2.4f)) - 0.055f;
		g = g <= 0.0031308f ? 12.92f * g : (1.0f + 0.055f) * (float)Math.pow(g, (1.0f / 2.4f)) - 0.055f;
		b = b <= 0.0031308f ? 12.92f * b : (1.0f + 0.055f) * (float)Math.pow(b, (1.0f / 2.4f)) - 0.055f;      

		
		if (r > b && r > g) {
			// red is biggest
			if (r > 1.0f) {
				g = g / r;
				b = b / r;
				r = 1.0f;
			}
		}
		else if (g > b && g > r) {
			// green is biggest
			if (g > 1.0f) {
				r = r / g;
				b = b / g;
				g = 1.0f;
			}
		}
		else if (b > r && b > g) {
			// blue is biggest
			if (b > 1.0f) {
				r = r / b;
				g = g / b;
				b = 1.0f;
			}
		}

		// neglecting if the value is negative.
		if(r<0.0f){
			r=0.0f;
		}
		if(g<0.0f){
			g=0.0f;
		}if(b<0.0f){
			b=0.0f;
		}

		// Converting float components to int components.
		int r1= (int) (r*255.0f);
		int g1= (int) (g*255.0f);
		int b1= (int) (b*255.0f);

		return Color.rgb( r1, g1, b1);
	}

	/**
	 * Return x, y & brightness value from android color & light model Id.
	 * @param color the color value
	 * @param model the model Id of Light
	 * @return float[] the float array of length 3, where index 0, 1 & 2 gives respective x, y and brightness values.
	 */
	public static float [] calculateXY(int color, String model) {    

		// Default to white
		float red = 1.0f;
		float green = 1.0f;
		float blue = 1.0f;
		
		//Get no. of components
		 red = Color.red(color) / 255.0f;
		 green = Color.green(color)  / 255.0f;
		 blue = Color.blue(color)  / 255.0f;

		// Wide gamut conversion D65
		float r = ((red   > 0.04045f) ? (float)Math.pow((red   + 0.055f) / (1.0f + 0.055f), 2.4f) : (red   / 12.92f));
		float g = (green > 0.04045f)  ? (float)Math.pow((green + 0.055f) / (1.0f + 0.055f), 2.4f) : (green / 12.92f);
		float b = (blue  > 0.04045f)  ? (float)Math.pow((blue  + 0.055f) / (1.0f + 0.055f), 2.4f) : (blue  / 12.92f);

		//Why values are different in ios and android  , IOS is considered
		// Modified conversion from RGB -> XYZ with better results on colors for the lights
		float X = r * 0.649926f + g * 0.103455f + b * 0.197109f;    
		float Y = r * 0.234327f + g * 0.743075f + b * 0.022598f;    
		float Z = r * 0.0000000f + g * 0.053077f + b * 1.035763f;

		float xy[] = new float[2];
		xy[0] = (float) (X / (X + Y + Z));
		xy[1] = (float) (Y / (X + Y + Z));
		if (Float.isNaN(xy[0])) {
			xy[0] = 0.0f;
		}
		if (Float.isNaN(xy[1])) {
			xy[1] = 0.0f;
		}
		//Check if the given XY value is within the colourreach of our lamps.
		PointF xyPoint = new PointF(xy[0],xy[1]);
		ArrayList<PointF> colorPoints=colorPointsForModel(model);
		boolean inReachOfLamps = checkPointInLampsReach(xyPoint,colorPoints);
		if (!inReachOfLamps) {
			//It seems the colour is out of reach
			//let's find the closes colour we can produce with our lamp and send this XY value out.

			//Find the closest point on each line in the triangle.
			PointF pAB =getClosestPointToPoints(colorPoints.get(CPT_RED), colorPoints.get(CPT_GREEN), xyPoint);  
			PointF pAC =getClosestPointToPoints(colorPoints.get(CPT_BLUE), colorPoints.get(CPT_RED), xyPoint);
			PointF pBC =getClosestPointToPoints(colorPoints.get(CPT_GREEN), colorPoints.get(CPT_BLUE), xyPoint);

			//Get the distances per point and see which point is closer to our Point.
			float dAB = getDistanceBetweenTwoPoints(xyPoint, pAB);
			float dAC = getDistanceBetweenTwoPoints(xyPoint, pAC);
			float dBC = getDistanceBetweenTwoPoints(xyPoint, pBC);

			float lowest = dAB;
			PointF closestPoint = pAB;
			if (dAC < lowest) { 
				lowest = dAC;
				closestPoint = pAC;
			}
			if (dBC < lowest) { 
				lowest = dBC;
				closestPoint = pBC;
			}

			//Change the xy value to a value which is within the reach of the lamp.
			xy[0] = closestPoint.x;
			xy[1] = closestPoint.y;
		}
	//	xy[2]=Y; // brightness
		return xy;
	}

	/**
	 * Method to see if the given XY value is within the reach of the lamps.
	 * 
	 * @param p the point containing the X,Y value
	 * @return true if within reach, false otherwise.
	 */
	private static boolean checkPointInLampsReach(PointF p, ArrayList<PointF> colorPoints) {        
		PointF red =   colorPoints.get(CPT_RED);    
		PointF green = colorPoints.get(CPT_GREEN);  
		PointF blue =  colorPoints.get(CPT_BLUE);     
		PointF v1 = new PointF(green.x - red.x, green.y - red.y);    
		PointF v2 = new PointF(blue.x - red.x, blue.y - red.y);        
		PointF q = new PointF(p.x - red.x, p.y - red.y);        
		float s = crossProduct(q ,v2) / crossProduct(v1,v2);    
		float t = crossProduct(v1 ,q) / crossProduct(v1 ,v2);     
		if ( (s >= 0.0f) && (t >= 0.0f) && (s + t <= 1.0f))    {        
			return true;    
		}else{        
			return false;    

		}
	}

	/**
	 * Find the distance between two points.
	 * 
	 * @param one
	 * @param two
	 * @return the distance between point one and two
	 */
	private static float getDistanceBetweenTwoPoints(PointF one, PointF two) {
		float dx = one.x - two.x; // horizontal difference
		float dy = one.y - two.y; // vertical difference
		float dist = FloatMath.sqrt(dx * dx + dy * dy);

		return dist;
	}

	/**
	 * Calculates crossProduct of two 2D vectors / points.
	 * 
	 * @param p1 first point used as vector
	 * @param p2 second point used as vector
	 * @return crossProduct of vectors
	 */
	private static float crossProduct(PointF p1, PointF p2){

		return (p1.x * p2.y - p1.y * p2.x);
	}
	private static ArrayList<PointF>  colorPointsForModel(String model) {    
		// LLC001, // LedStrip    // LWB001, // LivingWhite    
		if(model==null){ // if model is not known go for the default choice
			model=" "; 
		}
		ArrayList<PointF> colorPoints = new ArrayList<PointF>();      
		
		
		ArrayList<String> hueBulbs =new ArrayList<String>();
		hueBulbs.add("LCT001");

		ArrayList<String> livingColors  =new ArrayList<String>();
		livingColors.add("LLC001");
		livingColors.add("LLC005");
		livingColors.add("LLC006");
		livingColors.add("LLC007");
		livingColors.add("LLC010");
		livingColors.add("LLC011");
		livingColors.add("LLC012");


		if (hueBulbs.contains(model)) { 
			// Hue bulbs color gamut triangle
			colorPoints.add(new PointF(.674F, 0.322F));     // Red        
			colorPoints.add(new PointF(0.408F, 0.517F));    // Green        
			colorPoints.add(new PointF(0.168F, 0.041F));      // Blue            
		}else  if  (livingColors.contains(model)) { 
			// LivingColors color gamut triangle
			colorPoints.add(new PointF(0.703F, 0.296F));     // Red        
			colorPoints.add(new PointF(0.214F, 0.709F));   // Green        
			colorPoints.add(new PointF(0.139F, 0.081F));      // Blue    
		}else {        
			// Default construct triangle wich contains all values        
			colorPoints.add(new PointF(1.0F, 0.0F));// Red        
			colorPoints.add(new PointF(0.0F, 1.0F)); // Green       
			colorPoints.add(new PointF(0.0F, 0.0F));// Blue    
		}    
		return colorPoints;
	} 

	/**
	 * Find the closest point on a line.
	 * This point will be within reach of the lamp.
	 * 
	 * @param A the point where the line starts
	 * @param B the point where the line ends
	 * @param P the point which is close to a line.
	 * @return the point which is on the line.
	 */
	private static PointF getClosestPointToPoints(PointF A, PointF B,PointF P) {    
		PointF AP = new PointF(P.x - A.x, P.y - A.y);    
		PointF AB = new PointF(B.x - A.x, B.y - A.y);    
		float ab2 = AB.x*AB.x + AB.y*AB.y;    
		float ap_ab = AP.x*AB.x + AP.y*AB.y;        
		float t = ap_ab / ab2;        
		if (t < 0.0f)        
			t = 0.0f;    
		else if (t > 1.0f)        
			t = 1.0f;        
		PointF newPoint = new PointF(A.x + AB.x * t, A.y + AB.y * t);    
		return newPoint;
	}

	/*public float[] getColorComponents(int color)
	{       
		int alpha = (color >> 24) & 0xff;
		int red = (color >> 16) & 0xff;
		int green = (color >> 8) & 0xff;
		int blue = color & 0xff;
		float a = (float) alpha / 255;
		float r = (float) red / 255;
		float g = (float) green / 255;
		float b = (float) blue / 255;
		return new float[] { a, r, g, b };
	}*/
}
