package org.dmonakhov.mentprotector;

public final class Id {
	public static final class menu {
		public static final int menu_setup = 0x21080001;
		public static final int menu_quit =  0x21080002;
	}
	
	public static final class Service {
		public static final class State {
			public static final int IDLE 		= 0x21080010;
			public static final int STOPPED		= 0x21080011;
			public static final int RECORDING 	= 0x21080012;
			public static final int ERROR		= 0x21080013;
		}
		
		public static final class Messages
		{
			public static final int MSG_REGISTER_CLIENT 	= 0x21080100;
			public static final int MSG_UNREGISTER_CLIENT 	= 0x21080101;			
			public static final int MSG_START_RECORD 		= 0x21080102;
			public static final int MSG_STOP_RECORD 		= 0x21080103;
			public static final int MSG_GET_STATE			= 0x21080104;
			/* Replay to client's message */
			public static final int MSG_STATE_UPDATE		= 0x21080180;
		}
	}
	
	public static final class Config
	{
		public static final int current_version = 0x1;
		
		public static final int QUALITY_LOW		= 0x0;
		public static final int QUALITY_MEDIUM	= 0x1;
		public static final int QUALITY_HIGH	= 0x2;
	};
}
