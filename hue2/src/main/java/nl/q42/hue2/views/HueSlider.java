package nl.q42.hue2.views;

import nl.q42.hue2.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HueSlider extends View {
	private Bitmap background;
	private Paint paint;
	
	private Rect backgroundRect, viewRect, contentRect;
	
	private float hue = 180.0f;
	
	private SatBriSlider satBri;
	
	public HueSlider(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Load hue background
		background = BitmapFactory.decodeResource(context.getResources(), R.drawable.hue_slider);
	}
	
	public void setSatBriSlider(SatBriSlider slider) {
		satBri = slider;
		satBri.setColor(getRGBColor());
	}
	
	private int getRGBColor() {
		return background.getPixel(0, (int) (hue / 360.0f * 512.0f));	
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (paint == null) {
			paint = new Paint();
			paint.setARGB(255, 0, 0, 0);
			paint.setStrokeWidth(4);
			
			backgroundRect = new Rect(0, 0, background.getWidth(), background.getHeight());
			viewRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
			contentRect = new Rect(2, 2, canvas.getWidth() - 2, canvas.getHeight() - 2);
		}
		
		// Draw border
		canvas.drawRect(viewRect, paint);
		
		// Draw colors background
		canvas.drawBitmap(background, backgroundRect, contentRect, paint);
		
		// Draw selector
		int hueY = (int) ((float) canvas.getHeight() / 360.0f * hue);
		canvas.drawLine(0, hueY, canvas.getWidth(), hueY, paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				hue = Math.max(0.0f, Math.min(event.getY() / viewRect.bottom * 360.0f, 359.0f));
				if (satBri != null) satBri.setColor(getRGBColor());
				invalidate();
				break;
		}
		
		return true;
	}
}
