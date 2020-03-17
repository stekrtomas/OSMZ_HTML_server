package com.example.serverhttp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class HttpServerActivity extends Activity implements OnClickListener{

	private SocketServer s;

	/*Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){

		}
	};*/;
	TextView textView; // proměnná pro výpis logu z vláken
	private EditText editText; //proměnná pro počet povolených vláken
	Button btn1;
	Button btn2;
	Button btn3;
	Camera camera;

	Handler handler = new Handler(Looper.getMainLooper()){
		@Override
		public void handleMessage(Message inputMessage) {
			// Gets the image task from the incoming Message object.
			textView.append("Cesta k souboru: " + inputMessage.getData().getCharSequence("LOG"));
		}
	};


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_server);
        
        btn1 = (Button)findViewById(R.id.button1);
        btn2 = (Button)findViewById(R.id.button2);
        btn2.setEnabled(false);
		textView = findViewById(R.id.textView);
		editText = findViewById(R.id.editNumber);

		btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        textView.setText("Výpis logu vlákna!\n");
		editText.setText("1");

		camera = getCameraInstance();
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int threadCount;
		if (v.getId() == R.id.button1) {
			if(!checkCameraHardware(this.getApplicationContext())){
				Log.d("SERVER", "Device have no camera! ");
			}
			else{
				Log.d("SERVER", "Camara succesfully opened. ");
				camera = getCameraInstance();
			}
			btn1.setEnabled(false);
			btn2.setEnabled(true);
			if(editText.getText().toString().equals("")){
				threadCount = 1;
			}else{
				threadCount = Integer.parseInt(editText.getText().toString());
			}
			s = new SocketServer(handler,threadCount,this.getApplicationContext(), camera);

			s.start();
		}
		if (v.getId() == R.id.button2) {
			btn1.setEnabled(true);
			btn2.setEnabled(false);
			s.close();
			try {
				s.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			c=null;
		}
		return c; // returns null if camera is unavailable
	}

    
}
