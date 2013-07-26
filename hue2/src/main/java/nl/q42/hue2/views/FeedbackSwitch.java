package nl.q42.hue2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Switch that properly handles the difference between user input and programmatic checking for callbacks
 */
public class FeedbackSwitch extends Switch {
	private boolean ignoreCallback = false;
	
	public FeedbackSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// TODO: This doesn't seem to work well at all
	public void setChecked(boolean checked, boolean ignoreCallback) {
		this.ignoreCallback = ignoreCallback;
		setChecked(checked);
		this.ignoreCallback = false;
	}
	
	@Override
	@Deprecated
	public void setChecked(boolean checked) {
		super.setChecked(checked);
	}
	
	@Override
	public void setOnCheckedChangeListener(final OnCheckedChangeListener listener) {
		super.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (!ignoreCallback) {
					listener.onCheckedChanged(buttonView, isChecked);
				}
			}
		});
	}
}
