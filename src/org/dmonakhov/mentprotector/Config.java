package org.dmonakhov.mentprotector;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;

public final class Config {
	public static final String TAG = "Config";
	/**
     * These values are used to read and write the properties file.
     * PROPERTY_DELIMITER delimits the key and value in a Java properties file.
     * The "marker" strings are used to write the properties into the file
     */
    public static final String PROPERTY_DELIMITER = "=";
    public static final String PREFERENCES_FILE = "MentProtectorPrefs";
    public ConfigInfo info;
    /**
     * The key or label for "position" in the preferences file
     */
    public static final String ConfigVersion = "ConfigVersion";
    public static final String CameraIdKey  = "CameraId";
    public static final String CameraSizeKey = "CameraSizeId";
    public static final String CameraQualityKey = "CameraQualityId";
    public static final String AudioQualityKey = "AudioQualityId";
    public static final String StreamSupportKey = "StreamSupport";
    
    public Config ()
    {
    	info = new ConfigInfo();
    	info.configVersion = Id.Config.current_version;
    	info.cameraId = 0;
    	info.cameraSizeId =  0;
    	info.cameraQualityId = 1;
    	info.audioQualityId = Id.Config.QUALITY_MEDIUM;
    	info.streamSupportId =Id.Config.QUALITY_MEDIUM ;
    }
	public class ConfigInfo
	{
		public int configVersion;
	    public int cameraId;
	    public int cameraSizeId;
	    public int cameraQualityId;
	    public int audioQualityId;
	    public int streamSupportId;
	};

	/* This is very strange, but Shared preferences has to automatic conversion from Bundle.
	 * I need both (SP and Bundle )interfaces, so stupid hardcoding is needed until new solution
	 * will be found.
	 */
	private void doP2B_Int(String key, int def, Bundle b, SharedPreferences p)
	{
		b.putInt(key, p.getInt(key,def));
	}
	private void doB2P_Int(String key, SharedPreferences.Editor e, Bundle b)
	{
		e.putInt(key, b.getInt(key));
	}
	public void fromBundle(Bundle b)
	{		
		info = new ConfigInfo();		
		info.configVersion = b.getInt(ConfigVersion, Id.Config.current_version);
        if (info.configVersion == Id.Config.current_version) {
        	info.cameraId = b.getInt(CameraIdKey, 0);
        	info.cameraSizeId = b.getInt(CameraSizeKey, 0);
        	info.cameraQualityId = b.getInt(CameraQualityKey, Id.Config.QUALITY_MEDIUM);
        	info.audioQualityId = b.getInt(AudioQualityKey, Id.Config.QUALITY_MEDIUM);
        	info.streamSupportId = b.getInt(StreamSupportKey, 1);
        } else {
        	info.configVersion = Id.Config.current_version;
        	info.cameraId = 0;
        	info.cameraSizeId =  0;
        	info.cameraQualityId = 1;
        	info.audioQualityId = Id.Config.QUALITY_MEDIUM;
        	info.streamSupportId =Id.Config.QUALITY_MEDIUM ;
        }	
	}
	public Bundle toBundle()
	{
		Bundle b = new Bundle();
        b.putInt(ConfigVersion, info.configVersion);
        b.putInt(CameraIdKey, info.cameraId);
        b.putInt(CameraSizeKey, info.cameraSizeId);
        b.putInt(CameraQualityKey, info.cameraQualityId);
        b.putInt(AudioQualityKey, info.audioQualityId);
        b.putInt(StreamSupportKey, info.streamSupportId);
        return b;
	}
	public void readCfg(Context c) {
		Bundle b = new Bundle();
		SharedPreferences p = c.getSharedPreferences(PREFERENCES_FILE, Activity.MODE_WORLD_READABLE);
		doP2B_Int(ConfigVersion, Id.Config.current_version, b, p);
        doP2B_Int(CameraIdKey, 0, b, p);
        doP2B_Int(CameraSizeKey, 0, b, p);
        doP2B_Int(CameraQualityKey, Id.Config.QUALITY_MEDIUM, b, p);
        doP2B_Int(AudioQualityKey, Id.Config.QUALITY_MEDIUM, b, p);
        doP2B_Int(StreamSupportKey, 1, b,p);
        this.fromBundle(b);
	}
	public void dump()
	{
		if (info == null)
			Log.e(TAG, "ERROR INFO IS EMPTY");
    	Log.v(TAG, "CameraId" + info.cameraId);
    	Log.v(TAG, "CameraSize" + info.cameraSizeId);
    	Log.v(TAG, "CameraQuality" + info.cameraQualityId);
    	Log.v(TAG, "AudioQuality" + info.audioQualityId);
    	Log.v(TAG, "StreamSupport" + info.streamSupportId);	
	}
	 /**
     * Write the application's current state to a properties repository.
     * @param c - The Activity's Context
     *
     */
    public boolean writeCfg(Context c)
    {
    	Bundle b = toBundle();
        SharedPreferences p = c.getSharedPreferences(PREFERENCES_FILE, Activity.MODE_WORLD_READABLE);
        SharedPreferences.Editor e = p.edit();
		doB2P_Int(ConfigVersion, e, b);
        doB2P_Int(CameraIdKey,e, b);
        doB2P_Int(CameraSizeKey, e, b);
        doB2P_Int(CameraQualityKey, e, b);
        doB2P_Int(AudioQualityKey, e, b);
        doB2P_Int(StreamSupportKey, e, b);
        return (e.commit());
    }
}
