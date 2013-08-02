package nl.q42.hue2.views;

import nl.q42.hue2.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Button with a colored background
 */
public class ColorButton extends Button {
	private Bitmap template;
	
	public ColorButton(Context context) {
		super(context);
		
		// Load white button background
		template = BitmapFactory.decodeResource(context.getResources(), R.drawable.btn_template);
	}
	
	public ColorButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Load white button background
		template = BitmapFactory.decodeResource(context.getResources(), R.drawable.btn_template);
	}
	
	@SuppressWarnings("deprecation")
	public void setColor(int color) {
		// Extract template pixels
		int w = template.getWidth();
		int h = template.getHeight();
		int[] pixels = new int[w * h];
		template.getPixels(pixels, 0, w, 0, 0, w, h);
		
		// Scale pixels by color
		float r = Color.red(color) / 255.0f;
		float g = Color.green(color) / 255.0f;
		float b = Color.blue(color) / 255.0f;
		
		for (int i = 0; i < w * h; i++) {
			int col = pixels[i];
			
			pixels[i] = Color.argb(
				Color.alpha(col),
				(int) (r * Color.red(col)),
				(int) (g * Color.green(col)),
				(int) (b * Color.blue(col))
			);
		}
		
		// Set result as bitmap
		Bitmap bg = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		bg.setPixels(pixels, 0, w, 0, 0, w, h);
		
		NinePatchDrawable drawable = new NinePatchDrawable(getResources(), bg, template.getNinePatchChunk(), new Rect(), null);
		setBackgroundDrawable(drawable); // Non-deprecated version only on API >= 16
		
		invalidate();
	}
}
