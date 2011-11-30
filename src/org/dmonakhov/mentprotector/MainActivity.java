package org.dmonakhov.mentprotector;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Camera;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
    private static final String TAG = "MainActivity";
	public static SurfaceView mSurfaceView;
        public static SurfaceHolder mSurfaceHolder;
        public static Camera mCamera;
        public static boolean mPreviewRunning;
        public static int mState = 0;
        private Button recButton;
         /** Messenger for communicating with service. */
        Messenger mService = null;
        /** Flag indicating whether we have called bind on the service. */
        boolean mIsBound;
        /** Some text view we are using to show state information. */
        TextView mCallbackText;
        
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
        Log.i(TAG, "surfaceView:" + mSurfaceView  + "  holder:" + mSurfaceHolder);
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Intent intent = new Intent(MainActivity.this, RecorderService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);
        doBindService();

        recButton = (Button) findViewById(R.id.RecButton);
        
        mCallbackText = (TextView)findViewById(R.id.text_info);
	    mCallbackText.setText("Can not send message");
        recButton.setOnClickListener(new View.OnClickListener()
        {
        	public void onClick(View v)
                {
        		try {
        			int newState = Id.Service.Messages.MSG_GET_STATE;
        			switch(mState){
        			case Id.Service.State.STOPPED:
        				newState = Id.Service.Messages.MSG_START_RECORD;
        				break;
        			case Id.Service.State.RECORDING:
        				newState = Id.Service.Messages.MSG_STOP_RECORD;
        				break;    				
        				default:
        				break;
        			}
        			Log.i(TAG, "Old State:"+ mState + " CMD:"+ newState);
        			Message msg = Message.obtain(null, newState);
        			msg.replyTo = mMessenger;
        			mService.send(msg);
        			
        		} catch (RemoteException e) {
        		     mCallbackText.setText("Can not send message");
        			// In this case the service has crashed before we could even
        			// do anything with it; we can count on soon being
        			// disconnected (and then reconnected if it can be restarted)
        			// so there is no need to do anything here.
        		}

                }
        });
        //mCallbackText = (TextView)findViewById(R.id.text_info);
        mCallbackText.setText("Not attached.");
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, RecorderService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
 
    }
    private void doStateUpdate(int newState)
    {
    	Log.i(TAG, "Update State :" + mState  + "  Button:"+ recButton);
    	mState = newState;
    	if (recButton == null)
    		return;
    	Log.i(TAG, "Update State :" + mState );
    	switch (mState)
    	{
    		case Id.Service.State.STOPPED:
    			recButton.setBackgroundColor(Color.GREEN);
    			break;
    		case Id.Service.State.RECORDING:
    			recButton.setBackgroundColor(Color.RED);
    			break;
    		case Id.Service.State.ERROR:
    			recButton.setBackgroundColor(Color.MAGENTA);
    			break;
    	}
    	
    }
    /**
     *Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
                case Id.Service.Messages.MSG_STATE_UPDATE:
                	mCallbackText.setText("State Updated to: " + msg.arg1);
                	doStateUpdate(msg.arg1);
                	
                	break;
                default:
                	super.handleMessage(msg);
    		}
    	}
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
    	public void onServiceConnected(ComponentName className,
    			IBinder service) {
    		// This is called when the connection with the service has been
    		// established, giving us the service object we can use to
    		// interact with the service.  We are communicating with our
    		// service through an IDL interface, so get a client-side
    		// representation of that from the raw service object.
    		mService = new Messenger(service);
    		mCallbackText.setText("Attached.");

    		// We want to monitor the service for as long as we are
    		// connected to it.
    		try {
    			Message msg = Message.obtain(null,
    					Id.Service.Messages.MSG_REGISTER_CLIENT);
    			msg.replyTo = mMessenger;
    			mService.send(msg);

    		} catch (RemoteException e) {
    			// In this case the service has crashed before we could even
    			// do anything with it; we can count on soon being
    			// disconnected (and then reconnected if it can be restarted)
    			// so there is no need to do anything here.
    		}

    		// As part of the sample, tell the user what happened.
    		Toast.makeText(MainActivity.this, "Service Connected",
    				Toast.LENGTH_SHORT).show();
    	}

    	public void onServiceDisconnected(ComponentName className) {
    		// This is called when the connection with the service has been
    		// unexpectedly disconnected -- that is, its process crashed.
    		mService = null;
    		mCallbackText.setText("Disconnected.");

    		// As part of the sample, tell the user what happened.
    		Toast.makeText(MainActivity.this, "Service Disconnected",
    				Toast.LENGTH_SHORT).show();
    	}
    };

    void doBindService() {
    	// Establish a connection with the service.  We use an explicit
    	// class name because there is no reason to be able to let other
    	// applications replace our component.
    	///// FIXME
    	bindService(new Intent(MainActivity.this, 
    			RecorderService.class), mConnection, Context.BIND_AUTO_CREATE);
    	mIsBound = true;
    	if (mCallbackText == null)
    		mCallbackText = (TextView)findViewById(R.id.text_info);
    	mCallbackText.setText("Binding.");
    }

    void doUnbindService() {
    	if (mIsBound) {
    		// If we have received the service, and hence registered with
    		// it, then now is the time to unregister.
    		if (mService != null) {
    			try {
    				Message msg = Message.obtain(null,
    						Id.Service.Messages.MSG_UNREGISTER_CLIENT);
    				msg.replyTo = mMessenger;
    				mService.send(msg);
    			} catch (RemoteException e) {
    				// There is nothing special we need to do if the service
    				// has crashed.
    			}
    		}

    		// Detach our existing connection.
    		unbindService(mConnection);
    		mIsBound = false;
    		mCallbackText.setText("Unbinding.");
    	}
    }
   
    public void  doCleanup()
    {
    	//// FIXME: Redesign service stop
    	stopService(new Intent(MainActivity.this, RecorderService.class));
    	finish();
    }
    
    //// Surface stuff
    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mSurfaceHolder = mSurfaceView.getHolder();
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }
    
    ///// Menu Stuff //////////////
    /** press-hold/context menu */
    public void onPopulateContextMenu(ContextMenu menu, View view, Object o) {
      populateMenu(menu);
    }

    /** when press-hoption selected */
    @Override public boolean onContextItemSelected(MenuItem item) {
      return applyMenuChoice(item) || super.onContextItemSelected(item);
    }

    /** respond to menu item selection */
    public boolean applyMenuChoice(MenuItem item) {
    	switch (item.getItemId()) {
        	case Id.menu.menu_setup:
        		startActivity(new Intent(this, SetupActivity.class));
        		return true;
        	case Id.menu.menu_quit:
        		doCleanup();
        		return true;
    	}
        return false;
    }

	/** create the menu items */
	public void populateMenu(Menu menu) {
	  menu.add(0, Id.menu.menu_setup, 0, R.string.menu_setup).setIcon(android.R.drawable.ic_menu_preferences);
	  menu.add(0, Id.menu.menu_quit, 0, R.string.menu_quit).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
	}
	/** hook into menu button for activity */
	@Override public boolean onCreateOptionsMenu(Menu menu) {
	  populateMenu(menu);
	  return super.onCreateOptionsMenu(menu);
	}

	/** when menu button option selected */
	@Override public boolean onOptionsItemSelected(MenuItem item) {
	  return applyMenuChoice(item) || super.onOptionsItemSelected(item);
	}
}

