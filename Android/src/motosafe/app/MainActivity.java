package motosafe.app;

/*
 * @author: Valentín Sánchez Ramírez
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.R.string;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth2";
   
   
  //Para el BT
  TextView txtArduino;
  BluetoothAdapter bluetooth;
  Handler puente;
  BluetoothSocket clientSocket;
  BluetoothDevice myDevice = null;
  //SPP UUID service
  UUID uuid= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  
  

  // MAC-address de mis dispositivo BT
  private static String address = "98:D3:31:B1:CC:72";
  

  
	//TextView de la posicion del GPS
	private TextView lblLatitud;
	private TextView lblLongitud;
	private TextView lblPrecision;
	private TextView lblEstado;
	
	private LocationManager locManager;
	private LocationListener locListener;
	
	//boton SMS
	Button buttonSMS;
	
	
   
  
 
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
    
  
    
    //get Bluetooth adapter 
    bluetooth = BluetoothAdapter.getDefaultAdapter();		
    //Comprobamos si el BT esta encendido o apagado
    checkBTState(); 
    //Comprobamos si el GPS esta encendido o apagado
    checkGpsState() ;
    
    //Inicializacion del Handler
    inicioHandler();
    Log.d("ConnectToServerThread", "inicializo todo");
    
    
    
  }
  
  
////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//Incializar Handler
////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
  private void inicioHandler(){
	  puente = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				txtArduino.setText(msg.obj.toString());
			}
		};
  }
  
  
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Comprobar estado del BT
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void checkBTState() {
		//
		if (bluetooth == null) {
			System.out.println("Device do not suportt");
			Toast.makeText(this, "Device does not support Bluetooth",
					Toast.LENGTH_LONG).show();
		}

		String toastText;
		if (bluetooth.isEnabled()) {
			String address = bluetooth.getAddress();
			String name = bluetooth.getName();
			toastText = name + " : " + address;
			
		} else {
			toastText = "Bluetooth disabled";
		}
		System.out.println(toastText);
		Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

		Set<BluetoothDevice> bondedDevices = bluetooth.getBondedDevices();
		BluetoothDevice myDevice = null;
		for (BluetoothDevice bluetoothDevice : bondedDevices) {
			if (bluetoothDevice.getName().equals("HC-05")) {
				myDevice = bluetoothDevice;
				Toast.makeText(this, "Encontrado!", Toast.LENGTH_SHORT).show();
				break;
			}
		}
		if (myDevice == null) {
			Toast.makeText(this, "No paired device found", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		
		
		try {
			System.out.println("conectando");
			clientSocket = myDevice.createRfcommSocketToServiceRecord(uuid);
			clientSocket.connect();
			System.out.println("enviando");
			DataOutputStream outputChannel =
			new DataOutputStream(clientSocket.getOutputStream());
			outputChannel.writeChars("\nConectado al dispositivo android E \r\n");
			outputChannel.flush();
			
			new RecibirComando(clientSocket).start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Toast.makeText(this, "Conectado!", Toast.LENGTH_SHORT).show();
	}
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////	
  //Comprobar estado GPS
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void checkGpsState() {
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
  	    	if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
  	    		Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
  	    	}else{
  	    		showGPSDisabledAlertToUser();
  	    	}
	}
	
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //Mensaje que se visualiza para permitir el encendido del GPS
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
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
  
 


  @Override
  public void onResume() {
    super.onResume();
    
    
    //comenzarLocalizacion();
    
  }
 
  @Override
  public void onPause() {
    super.onPause();   
 
  }
   
  
  

  

  ///////////////////////////////////////////////////////////////////
  //Enviar comando a Arduino
  ///////////////////////////////////////////////////////////////////
	public void enviarComando(String comando){
		DataOutputStream outputChannel;
		try {
			
			outputChannel = new DataOutputStream(clientSocket.getOutputStream());
			outputChannel.writeChars(comando);
			outputChannel.flush();
			Log.i("enviando comando", comando);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//Clase encargada de encontrar el dispositivo que queremos, conectarnos a el e inicializar 
	//la hebra de comunicacion
	/////////////////////////////////////////////////////////////////////////////////////////////
	
private class RecibirComando extends Thread{
		
		BluetoothSocket clientSocket;
		
		public RecibirComando(BluetoothSocket client) {
			clientSocket=client;
		}
		
		@Override
		public void run() {
			System.out.println("recibiendo");
			BufferedReader input;
			while(true){
			try {
				input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				System.out.println("hebra de recepcion");
				
				String a= input.readLine();
				System.out.println("hebra de recepcion final");
				Message msg = new Message();
				msg.obj= a;
				puente.sendMessage(msg);
				System.out.println(a);
			} catch (IOException e) {
				e.printStackTrace();
			}
			}
			
		}
		
		
	}
 
}