package nl.q42.hue2.views;

import nl.q42.hue2.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SatBriSlider extends View {
	private Bitmap foreground;
	private Paint paint;
	
	private Rect foregroundRect, viewRect, contentRect;
	private int w, h;
	
	private float saturation = 0.95f;
	private float brightness = 0.95f;
	
	private int color = Color.BLACK;
	
	public SatBriSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Load saturation and brightness foreground
		foreground = BitmapFactory.decodeResource(context.getResources(), R.drawable.sat_bri_foreground);
	}
	
	public void setHueColor(int color) {
		this.color = color;
		invalidate();
	}
	
	public int getResultColor() {
		// Calculate final color with hue, saturation and brightness sliders
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		
		hsv[1] = saturation;
		hsv[2] = brightness;
		
		return Color.HSVToColor(hsv);
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;
    }
	
	@Override
	public void onDraw(Canvas canvas) {
		if (paint == null) {
			paint = new Paint();
			paint.setStrokeWidth(2);
			
			foregroundRect = new Rect(0, 0, foreground.getWidth(), foreground.getHeight());
			viewRect = new Rect(0, 0, w, h);
			contentRect = new Rect(2, 2, w - 2, h - 2);
		}
		
		// Draw border
		paint.setARGB(255, 0, 0, 0);
		canvas.drawRect(viewRect, paint);
		
		// Draw shaded colors
		paint.setARGB(255, Color.red(color), Color.green(color), Color.blue(color));
		canvas.drawRect(contentRect, paint);
		
		paint.setARGB(255, 255, 255, 255);
		canvas.drawBitmap(foreground, foregroundRect, contentRect, paint);
		
		// Draw selection circle
		int x = (int) (saturation * viewRect.right);
		int y = (int) ((1.0f - brightness) * viewRect.bottom);
		
		paint.setStyle(Paint.Style.STROKE);
		paint.setARGB(255, 0, 0, 0);
		canvas.drawCircle(x, y, 8, paint);
		paint.setARGB(255, 255, 255, 255);
		canvas.drawCircle(x, y, 10, paint);
		paint.setARGB(255, 0, 0, 0);
		canvas.drawCircle(x, y, 12, paint);
		paint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				saturation = Math.max(0.0f, Math.min(event.getX() / viewRect.right, 1.0f));
				brightness = Math.max(0.0f, Math.min(1.0f - event.getY() / viewRect.bottom, 1.0f));
				invalidate();
				break;
		}
		
		return true;
	}
}
