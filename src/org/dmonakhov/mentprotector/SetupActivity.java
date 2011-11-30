package org.dmonakhov.mentprotector;

import java.util.List;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class SetupActivity extends Activity {
	static final String TAG= "SetupActivity";
	
	//// FIXME: Some strange things happens, ecplipse do not allow me to use string-array variables
	//// it can't find R.array.xxx, this is strange, so let's temroraly hardcode values here.
	static final String [] setup_quality_type = {"Low", "Medium", "High"};
	static final String [] setup_camera_info = {"Facing back", "Facing front", "Disabled" };
	static final String [] no_yes = {"No", "Yes"};
	static final String [] off_on = {"Off", "On"};
	//////////////////////////////////////////////

	private Config mCfg;
    protected ArrayAdapter<CharSequence> mQualityAdaptor;
    protected ArrayAdapter<CharSequence> mCamerasAdaptor;
    protected ArrayAdapter<CharSequence> mSizeAdaptor;
    

    
    private CameraData mCameras;
    private class CameraData {
    	int size;
    	CameraInfo[] info;
    	String[] name;
    	/* current camera info */
    	String[] sizeString;
    	List<Size> sizeList;

    	public CameraData()
    	{
    		String pref;
    		this.size = Camera.getNumberOfCameras();
    		Log.i(TAG, "Number of cameras " + size);
    		name = new String[size];
    		info = new CameraInfo[size];
    		for (int i = 0; i < size; i++) {
    			Log.i(TAG, "Try to capture cam info" + i);
    			info[i] = new CameraInfo();
    			Camera.getCameraInfo(i, info[i]);
    			if (info[i].facing == Camera.CameraInfo.CAMERA_FACING_BACK)
    				pref = "Rear";
    			else
    				pref = "Front";
    					
    			name[i] = pref + " camera["+ i+"]";
    		}
    		updateId(0);
    	}
    	
    	public int updateId(int id) {
    		if (id < 0 || id >= size)
    			return -1;
    		Log.i(TAG, "Update camera " + id);
    		Camera camera = Camera.open(id);
    		sizeList = camera.getParameters().getSupportedPreviewSizes();
    		camera.release();
    		sizeString = new String[sizeList.size()];
    		for (int i = 0; i < sizeList.size(); i++) {
    			sizeString[i] = sizeList.get(i).width + "x" + sizeList.get(i).height;
    			Log.i(TAG, "Sizees " + i + " sz:" + sizeString[i]);
    		}
    		return 0;
    	}
    };
    
    public void updateCameraId(int id)
    {
    	
    	Spinner cameraSize = (Spinner) findViewById(R.id.setup_camera_size_spinner);
    	int old_size = cameraSize.getSelectedItemPosition();
    	int ret = mCameras.updateId(id);
    	if (ret != 0)
    		return;
    	mSizeAdaptor = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, mCameras.sizeString);
    	cameraSize.setAdapter(mSizeAdaptor);

    	if (old_size >= cameraSize.getCount()) 
    		cameraSize.setSelection(0);
    	else
    		cameraSize.setSelection(old_size);
    	
    	Log.i(TAG, "Done updateCameraId ");
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {

        /**
         * derived classes that use onCreate() overrides must always call the super constructor
         */
        super.onCreate(savedInstanceState);
        mCfg = new Config();

        setContentView(R.layout.setup_menu);
        mQualityAdaptor = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, setup_quality_type);
        mCameras = new CameraData();
        mCamerasAdaptor = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, mCameras.name);
        mSizeAdaptor = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, mCameras.sizeString);


        Spinner cameraId = (Spinner) findViewById(R.id.setup_camera_id_spinner);
        Log.i(TAG, "cameraId:" + cameraId + " qualityAdapter:" +  mQualityAdaptor);
        cameraId.setAdapter(mCamerasAdaptor);
        cameraId.setOnItemSelectedListener(
        		new OnItemSelectedListener() {
        			public void onItemSelected(
        					AdapterView<?> parent, View view, int position, long id) {
        				Log.i(TAG, "camera id" + id);
        				mCfg.info.cameraId = position;
        				updateCameraId(mCfg.info.cameraId);
        			}
        			public void onNothingSelected(AdapterView<?> parent) {
        				Log.i(TAG, "camera id Nothing selected");
                    }
                });

        Spinner cameraSize = (Spinner) findViewById(R.id.setup_camera_size_spinner);
        this.updateCameraId(cameraId.getId());
        cameraSize.setOnItemSelectedListener(
        		new OnItemSelectedListener() {
        			public void onItemSelected(
        					AdapterView<?> parent, View view, int position, long id) {
        				Log.i(TAG, "CameraSize" + id);
        				mCfg.info.cameraSizeId = position;
        			}
        			public void onNothingSelected(AdapterView<?> parent) {
        				Log.i(TAG, "CameraSize Nothing selected");
                    }
                });
        
        
        Spinner cameraQuality = (Spinner) findViewById(R.id.setup_camera_quality_spinner);
        cameraQuality.setAdapter(mQualityAdaptor);
        cameraQuality.setOnItemSelectedListener(
        		new OnItemSelectedListener() {
        			public void onItemSelected(
        					AdapterView<?> parent, View view, int position, long id) {
        					mCfg.info.cameraQualityId = position;
        					
        					Log.i(TAG, "camera quality" + id );
        			}
        			public void onNothingSelected(AdapterView<?> parent) {
        				Log.i(TAG, "camera quality Nothing selected");
                    }
                });
        
        Spinner audioQuality = (Spinner) findViewById(R.id.setup_audio_quality_spinner);
        audioQuality.setAdapter(mQualityAdaptor);
        audioQuality.setOnItemSelectedListener(
        		new OnItemSelectedListener() {
        			public void onItemSelected(
        					AdapterView<?> parent, View view, int position, long id) {
        				Log.i(TAG, "audio quality" + id);
        				mCfg.info.audioQualityId = position;
        			}
        			public void onNothingSelected(AdapterView<?> parent) {
        				Log.i(TAG, "audio quality Nothing selected");
                    }
                });
    
    CheckBox streamSupportBox = (CheckBox) findViewById(R.id.setup_stream_support_box);
    streamSupportBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(TAG, (isChecked ? "Enabling" : "Disabling") + " StreamSupport");
            mCfg.info.streamSupportId = (isChecked) ? 1 : 0;
        
        }
    });
    updateValues();
    }
    /**
     * Restores the current state of the spinner (which item is selected, and the value
     * of that item).
     * Since onResume() is always called when an Activity is starting, even if it is re-displaying
     * after being hidden, it is the best place to restore state.
     *
     * Attempts to read the state from a preferences file. If this read fails,
     * assume it was just installed, so do an initialization. Regardless, change the
     * state of the spinner to be the previous position.
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
    	super.onResume();
    	updateValues();
    }
    public void updateValues() {
        /*
         * Try to read the preferences file. If not found, set the state to the desired initial
         * values.
         */

    	mCfg.readCfg(this);
    	mCfg.dump();

        updateCameraId(mCfg.info.cameraId);
        
        Spinner cameraSize = (Spinner)findViewById(R.id.setup_camera_size_spinner);
        cameraSize.setSelection(mCfg.info.cameraSizeId);


        Spinner cameraQuality = (Spinner)findViewById(R.id.setup_camera_quality_spinner);
        cameraQuality.setSelection(mCfg.info.cameraQualityId);
        
        Spinner audioQuality = (Spinner)findViewById(R.id.setup_audio_quality_spinner);
        audioQuality.setSelection(mCfg.info.audioQualityId);
        
       CheckBox streamSupportBox = (CheckBox) findViewById(R.id.setup_stream_support_box);
       streamSupportBox.setChecked((mCfg.info.streamSupportId != 0));

    }

    /**
     * Store the current state of the spinner (which item is selected, and the value of that item).
     * Since onPause() is always called when an Activity is about to be hidden, even if it is about
     * to be destroyed, it is the best place to save state.
     *
     * Attempt to write the state to the preferences file. If this fails, notify the user.
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();
        if (!mCfg.writeCfg(this)) {
             Toast.makeText(this,
                     "Failed to write state!", Toast.LENGTH_LONG).show();
          }
    }
  
}


