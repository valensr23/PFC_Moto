package motosafe.app;

/*
 * @author: Valentín Sánchez Ramírez
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth2";
   
  Button btnOn, btnOff;
  TextView txtArduino;
  Handler h;
   
  final int RECIEVE_MESSAGE = 1;		// Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder sb = new StringBuilder();
  
  private ConnectedThread mConnectedThread;
   
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address = "98:D3:31:B1:CC:72";
  
  //////////////////////////////////
  
	//TextView de la posicion del GPS
	private TextView lblLatitud;
	private TextView lblLongitud;
	private TextView lblPrecision;
	private TextView lblEstado;
	
	private LocationManager locManager;
	private LocationListener locListener;
	
	//boton SMS
	Button buttonSMS;
	///////////////////////////////////
   
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    setContentView(R.layout.activity_main);
    ///////////////////////////////
    
	lblLatitud = (TextView) findViewById(R.id.LblPosLatitud);
	lblLongitud = (TextView) findViewById(R.id.LblPosLongitud);
	lblPrecision = (TextView) findViewById(R.id.LblPosPrecision);
	lblEstado = (TextView) findViewById(R.id.LblEstado);
	//inicializo button y fijamos la accion que queremos que desarrolle
	buttonSMS = (Button)findViewById(R.id.buttonSMS);
	buttonSMS.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
			sendSMS("000","Mi posicion es latitud:" );
			
		}
	});
	////////////////////////////
    //btnOn = (Button) findViewById(R.id.btnOn);					// button LED ON
    //btnOff = (Button) findViewById(R.id.btnOff);				// button LED OFF
    txtArduino = (TextView) findViewById(R.id.txtArduino);		// for display the received data from the Arduino
    txtArduino.setText("Data from Arduino: no leemos");
    h = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
            case RECIEVE_MESSAGE:													// if receive massage
            	txtArduino.setText("Data from Arduino: entra a leer");
            	byte[] readBuf = (byte[]) msg.obj;
            	String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
            	sb.append(strIncom);												// append string
            	int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
            	if (endOfLineIndex > 0) { 											// if end-of-line,
            		String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                    sb.delete(0, sb.length());										// and clear
                	txtArduino.setText("Data from Arduino: " + sbprint); 	        // update TextView
                 
                }
            	//Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
            	break;
    		}
        };
	};
     
    btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
    checkBTState();

    
  //Comprobamos si el GPS esta encendido o apagado
  		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
  	    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
  	        Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
  	    }else{
  	        showGPSDisabledAlertToUser();
  	    }
  }
  
  /////////////////////////////////////////
  //Metodos mios
  ///////////////////////////////////////////////////////////////////////
  
  private void showGPSDisabledAlertToUser(){
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)	
		.setTitle("Activar GPS")	
		.setMessage("GPS esta desactivado. ¿Desea activarlo?")
	    .setPositiveButton("Menu opciones GPS",
	            new DialogInterface.OnClickListener(){
	        public void onClick(DialogInterface dialog, int id){
	            Intent callGPSSettingIntent = new Intent(
	                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	            startActivity(callGPSSettingIntent);
	        }
	    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
			//Salimos de la app
			finish();
			}

	    }).show();
	}
  
  
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo para obtener la LOCALIZACION del dispositivo
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private void comenzarLocalizacion()
{

//Obtenemos una referencia al LocationManager
locManager = 
(LocationManager)getSystemService(Context.LOCATION_SERVICE);

//Obtenemos la última posición conocida
Location loc = 
locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

//Mostramos la última posición conocida
mostrarPosicion(loc);

//Nos registramos para recibir actualizaciones de la posición
locListener = new LocationListener() {
public void onLocationChanged(Location location) {
mostrarPosicion(location);
}
public void onProviderDisabled(String provider){
lblEstado.setText("Provider OFF");
}
public void onProviderEnabled(String provider){
lblEstado.setText("Provider ON ");
}
//Actualizamos la posicion del GPS cada 30 segundos
public void onStatusChanged(String provider, int status, Bundle extras){
}
};

locManager.requestLocationUpdates(
LocationManager.GPS_PROVIDER, 30, 0, locListener);
}


private void mostrarPosicion(Location loc) {
/*Metodo que comprueba si la variable loc contiene información, en caso 
* contrario nos muestra el mensaje sin datos para latitud y longitud
* en caso contrario nos muestra la posición en la que nos encontramos
*/
if(loc != null)
{
lblLatitud.setText("Latitud: " + String.valueOf(loc.getLatitude()));
lblLongitud.setText("Longitud: " + String.valueOf(loc.getLongitude()));
lblPrecision.setText("Precision: " + String.valueOf(loc.getAccuracy()));
}
else
{
lblLatitud.setText("Latitud: (sin_datos)");
lblLongitud.setText("Longitud: (sin_datos)");
lblPrecision.setText("Precision: (sin_datos)");
}
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo envio SMS
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private void sendSMS (String phoneNumber, String message){
SmsManager sms = SmsManager.getDefault();
sms.sendTextMessage(phoneNumber, null, message, null, null);
}

  
  
@Override
protected void onStop() {

/*Cuando la actividad es destruida, se ejecuta este método.
*/
super.onStop();

	

}
  
  
  ////////////////////////////////////////////////////////////////////////
  
  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
   
  @Override
  public void onResume() {
    super.onResume();
    comenzarLocalizacion();
    Log.d(TAG, "...onResume - try connect...");
   
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
   
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    /*
	try {
		btSocket = createBluetoothSocket(device);
	} catch (IOException e) {
		errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
	}*/
    
    try {
    
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }
   
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
   
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect(); 
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
     
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
    mConnectedThread = new ConnectedThread(btSocket);
    mConnectedThread.start();
  }
 
  @Override
  public void onPause() {
    super.onPause();
 
    Log.d(TAG, "...In onPause()...");
  
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
   
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
 
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }
 
  private class ConnectedThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	        	try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(String message) {
	    	Log.d(TAG, "...Data to send: " + message + "...");
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	          }
	    }
	}
}