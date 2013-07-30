package nl.q42.hue2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.q42.hue2.models.Bridge;
import nl.q42.hue2.models.Preset;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PresetsDataSource {
	private SQLiteDatabase db;
	private DBHelper dbHelper;

	public PresetsDataSource(Context context) {
		dbHelper = new DBHelper(context);
	}
	
	public void open() throws SQLException {
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public int insertPreset(String bridge_serial, String light_id, String group_id, float[] xy_color, int brightness) {
		ContentValues values = new ContentValues();
		
		values.put("bridge_serial", bridge_serial);
		values.put("light_id", light_id);
		values.put("group_id", group_id);
		values.put("x_color", xy_color[0]);
		values.put("y_color", xy_color[1]);
		values.put("brightness", brightness);
		
		return (int) db.insert("presets", null, values);
	}
	
	public void removePreset(Preset preset) {
		db.delete("presets", "id=" + preset.id, null);
	}

	/**
	 * Return mapping from light IDs to color presets
	 */
	public Map<String, List<Preset>> getAllPresets(Bridge bridge) {
		Map<String, List<Preset>> presets = new HashMap<String, List<Preset>>();

		Cursor cursor = db.query("presets", dbHelper.allColumns(), "bridge_serial=?", new String[]{bridge.getSerial()}, null, null, null);
		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			Preset preset = cursorToPreset(cursor);
			
			if (!presets.containsKey(preset.light)) {
				presets.put(preset.light, new ArrayList<Preset>());
			}
			
			presets.get(preset.light).add(preset);
			
			cursor.moveToNext();
		}
		
		cursor.close();
		
		return presets;
	}

	private Preset cursorToPreset(Cursor cursor) {
		Preset preset = new Preset();
		
		preset.id = cursor.getInt(0);
		preset.light = cursor.getString(2);
		preset.group = cursor.getString(3);
		preset.xy = new float[] { cursor.getFloat(4), cursor.getFloat(5) };
		preset.brightness = cursor.getInt(6);
		
		return preset;
	}
}
