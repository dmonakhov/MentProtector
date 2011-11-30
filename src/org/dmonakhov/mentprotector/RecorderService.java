package org.dmonakhov.mentprotector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Messenger;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class RecorderService extends Service {
	private static final String TAG = "RecorderService";
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private static Camera mServiceCamera;
	private MediaRecorder mMediaRecorder;
	private int mState = Id.Service.State.STOPPED;
	public static Config mCfg;
	
	 NotificationManager mNM;
	    /** Keeps track of all current registered clients. */
	    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	private void doStateUpdate(int newState)
	{	
		mState = newState;
		for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null,
                        Id.Service.Messages.MSG_STATE_UPDATE, mState, 0));
                Log.i(TAG, "Reply MSG :"+ i+"  "+ mClients.get(i));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
	}
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i(TAG, "Got messg what:" + msg.what);
        	switch (msg.what) {

                case Id.Service.Messages.MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case Id.Service.Messages.MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case Id.Service.Messages.MSG_START_RECORD:
                	startRecording();
                	///// TODO duplicate MESSAGE;
                	doStateUpdate(mState);
                	break;                  
                case Id.Service.Messages.MSG_STOP_RECORD:
                	stopRecording();
                    break;
                case Id.Service.Messages.MSG_GET_STATE:
                	doStateUpdate(mState);
                	break;
                default:
                    super.handleMessage(msg);
            }
        	doStateUpdate(mState);
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());
	
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
	
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Init surface view: " + mSurfaceView);
		Log.i(TAG, "Init surface holder: " + mSurfaceHolder);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		
		stopRecording();		
		super.onDestroy();
	}   
	public boolean startRecording() {

		if (mState != Id.Service.State.STOPPED) { 
			Toast.makeText(getBaseContext(), "Searvice not ready", Toast.LENGTH_SHORT).show();
			return false;
		}
		try {
			/// TODO: Find beter way to pass surface holder via args
			mSurfaceView = MainActivity.mSurfaceView;
			mSurfaceHolder = MainActivity.mSurfaceHolder;
			mCfg = new Config();
			mCfg.readCfg(this);
			mCfg.dump();
			
			Toast.makeText(getBaseContext(), "Recording Started", Toast.LENGTH_SHORT).show();
			
			mServiceCamera = Camera.open(mCfg.info.cameraId);
			Camera.Parameters params = mServiceCamera.getParameters();
			mServiceCamera.setParameters(params);
			Camera.Parameters p = mServiceCamera.getParameters();
			
			final List<Size> listSize = p.getSupportedPreviewSizes();
			Size mPreviewSize = listSize.get(mCfg.info.cameraSizeId);

			p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

			mServiceCamera.setParameters(p);

			try {
				mServiceCamera.setPreviewDisplay(mSurfaceHolder);
				mServiceCamera.startPreview();
			}
			catch (IOException e) {
				
				Log.e(TAG, " CRAP holder:" + mSurfaceHolder + "  MSG" + e.getMessage());
				e.printStackTrace();
			}
			
			mServiceCamera.unlock();
			
			mMediaRecorder = new MediaRecorder();
			mMediaRecorder.setCamera(mServiceCamera);
			
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

			if (mCfg.info.streamSupportId == 1) {
				  /*
				   * Use hidden interface to write in to stream-able container(mpeg2ts).
				   * Currently it support only H264/AAC, but this enough for most situations
				   * defined in frameworks/base/include/media/mediarecorder.h
				   *  and in frameworks/base/media/java/android/media/MediaRecorder.java
				   *  @hide H.264/AAC data encapsulated in MPEG2/TS
				   *	public static final int OUTPUT_FORMAT_MPEG2TS = 8;
				   *  AAC audio codec
				   *    public static final int AAC = 3
				   */
				mMediaRecorder.setOutputFormat(8);
				mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
				mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
				/// TODO Write output to socket fd
				mMediaRecorder.setOutputFile("/sdcard/test2.ts");
			} else {
				mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
				mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
				mMediaRecorder.setOutputFile("/sdcard/" + System.currentTimeMillis() + ".mp4");
			}
			
			mMediaRecorder.setVideoFrameRate(30);
			/* Black Magic start's here
			 * I don't know any good way to determine reasonable bit-rate.
			 * So let's assume that comression_level is  1 << ( 3 - CameraQuality)
			 *  {x8:LOW}, {x4:MEDIUM}, {x2:HIGH})
			 *  RAW_BITRATE =  H * W * FPS * bytes_per_pixel(let's hard-code it as 2)
			 *  bitRate = RAW_BITRATE / comression_level
			 */
			int bitRate = (mPreviewSize.width * mPreviewSize.height* 2 * 30) >> (3 - mCfg.info.cameraQualityId);

			mMediaRecorder.setVideoEncodingBitRate(bitRate);
			mMediaRecorder.setVideoSize(mPreviewSize.width, mPreviewSize.height);
			
			switch(mCfg.info.audioQualityId) {
				case Id.Config.QUALITY_LOW:
					mMediaRecorder.setAudioSamplingRate(8000);
					mMediaRecorder.setAudioEncodingBitRate(16*1024);
					break;
				case Id.Config.QUALITY_MEDIUM:
					mMediaRecorder.setAudioSamplingRate(22050);
					mMediaRecorder.setAudioEncodingBitRate(32*1024);
					break;
				case Id.Config.QUALITY_HIGH:
					mMediaRecorder.setAudioSamplingRate(44100);
					mMediaRecorder.setAudioEncodingBitRate(64*1024);
					break;
			}

			Log.i(TAG, "BitRate " + bitRate);
			try {
				mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
			} catch (Exception e){
				    Log.e(TAG, " CRAP2 holder:" + mSurfaceHolder + "  MSG" + e.getMessage());
					e.printStackTrace();
					return false;
			}
			try {
				mMediaRecorder.prepare();
			} catch (Exception e){
			    Log.e(TAG, " CRAP3 holder:" + mSurfaceHolder + "  MSG" + e.getMessage());
				e.printStackTrace();
				return false;
		    }
			try {
				mMediaRecorder.start();
			} catch (Exception e){
			    Log.e(TAG, " CRAP4 holder:" + mSurfaceHolder + "  MSG" + e.getMessage());
				e.printStackTrace();
				return false;
			}
			
			doStateUpdate(Id.Service.State.RECORDING);
			return true;
		} catch (IllegalStateException e) {
			Log.d(TAG, e.getMessage());
			e.printStackTrace();
			return false;
			
		}
	}

	public void stopRecording() {
		if (mState != Id.Service.State.RECORDING)
			return;
		Toast.makeText(getBaseContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
		try {
			mServiceCamera.reconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "ERROR" + e.getStackTrace());
		}
		
		mMediaRecorder.stop();
		mMediaRecorder.reset();
		
		mServiceCamera.stopPreview();
		mMediaRecorder.release();
		mServiceCamera.release();
		mServiceCamera = null;
		this.doStateUpdate(Id.Service.State.STOPPED);
	}
}
