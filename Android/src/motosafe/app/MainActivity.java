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
				byte[] buffer   = null;
			    String mensaje  = null;
			            buffer = (byte[])msg.obj;
			            mensaje = new String(buffer, 0, msg.arg1);
			            Log.d("algo falla aqui:",""+mensaje);
			            txtArduino.setText("estamos leyendo");
			            operoDatos(mensaje);
			            
			}
		};
  }
  

  private void operoDatos(String leido){

	  int pos1 = leido.indexOf("a");
	  int pos2 = leido.indexOf("b", pos1);
	  int pos3 = leido.indexOf("c", pos2);
	  int pos4 = leido.indexOf("d", pos3);
	  int pos5 = leido.indexOf(" ");
	  if(pos1 !=-1 && pos2 !=-1 && pos3!=-1 && pos4!=-1){
		  //String ax = (leido.substring(pos1+1, pos2));
		  //String ay = (leido.substring(pos2+1, pos3));
		  //String az = (leido.substring(pos3+1, pos4));
		  double x = Double.valueOf(leido.substring(pos1+1, pos2));
		  double y = Double.valueOf(leido.substring(pos2+1, pos3));
		  double z = Double.valueOf(leido.substring(pos3+1, pos4));
		  //Log.d("algo falla aqui x:",""+x);
		  //Log.d("algo falla aqui y:",""+y);
		  //Log.d("algo falla aqui z:",""+z);
		  //lblLongitud.setText(ax);
		  //lblPrecision.setText(ay);
		  //lblEstado.setText(az);
		  algoritmoEmergencia(x, y, z);
	  }
  }
  
  
  
  private void algoritmoEmergencia(double x, double y, double z){
	  double theta;
	  double phi;
	  //constante para que pi sea 180 grados
	  double ang = (180/3.1415);
	  
	  theta = Math.atan2(x, z)*ang;
	  phi = Math.atan2(y, z);
	  lblLatitud.setText("theta:"+theta);
	  lblLongitud.setText("no me he caido");
	  if(theta>55 || theta<-55){
		  checkSpeed();
		  lblLongitud.setText("es posible que me haya caido");
	  }
  }
  
  

  private void checkSpeed(){
	  
	  	//Obtenemos una referencia al LocationManager
		locManager = 
		(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		//Obtenemos la última posición conocida
		Location loc = 
		locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		
		//Nos registramos para recibir actualizaciones de la posición
		locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				//tomamos la ubicacion actual y medimos la velocidad a la que nos encontramos
				lblPrecision.setText("calculando velocidad 1");
				double latitud = location.getLatitude();
				double longitud = location.getLongitude();
				double precision = location.getAccuracy();
				float speed = location.getSpeed();
				lblPrecision.setText("calculando velocidad");
				if(speed < 3){
					lblPrecision.setText("me he caido :(");
					sendSMS("112","Me encuentro en", latitud, longitud, precision);
					
				}
				
			}

			@Override
			public void onProviderDisabled(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				// TODO Auto-generated method stub
				
			}
		};
		
		locManager.requestLocationUpdates(
		LocationManager.GPS_PROVIDER, 30, 0, locListener);
		
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
  

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo envio SMS
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private void sendSMS (String phoneNumber, String message, double latitud, double longitud, double precision){
	SmsManager sms = SmsManager.getDefault();
	String latitudS = Double.toString(latitud);
	String longitudS = Double.toString(longitud);
	String precisionS = Double.toString(precision);
	message = "he sufrido un accidente, me encuentro en latitud:"+latitudS+"longitud:"+longitudS+"precision:"+precisionS;
	lblEstado.setText(message);
	sms.sendTextMessage(phoneNumber, null, message, null, null);
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  
  
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
		private final InputStream inputStream;    // Flujo de entrada (lecturas)
	    private final OutputStream outputStream;   // Flujo de salida (escrituras)
	    
		
		public RecibirComando(BluetoothSocket client) {
			clientSocket=client;
			// Se usan variables temporales debido a que los atributos se declaran como final
			// no seria posible asignarles valor posteriormente si fallara esta llamada
			InputStream tmpInputStream = null;
			OutputStream tmpOutputStream = null;
			 
			// Obtenemos los flujos de entrada y salida del socket.
			try {
			    tmpInputStream = client.getInputStream();
			    tmpOutputStream = client.getOutputStream();
			    }
			    catch(IOException e){
			        Log.e(TAG, "HiloConexion(): Error al obtener flujos de E/S", e);
			    }
			        inputStream = tmpInputStream;
			        outputStream = tmpOutputStream;
			}
		
		
		//Metodo principal del hilo, encargado de realizar las lecturas
		public void run()
		{
			byte[] buffer = new byte[1024];
			int bytes;

			// Mientras se mantenga la conexion el hilo se mantiene en espera ocupada
			// leyendo del flujo de entrada
			while(true)
			{
				try {
					Thread.sleep(600);
					// Leemos del flujo de entrada del socket
					bytes = inputStream.read(buffer);
					
					// Enviamos la informacion a la actividad a traves del handler.
					// El metodo handleMessage sera el encargado de recibir el mensaje
					// y mostrar los datos recibidos en el TextView
					puente.obtainMessage(1, bytes, -1, buffer).sendToTarget();
				}
				catch(IOException e) {
					Log.e(TAG, "HiloConexion.run(): Error al realizar la lectura", e);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
		
		
	
 
