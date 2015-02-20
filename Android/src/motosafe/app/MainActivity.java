package motosafe.app;

/*
 * Proyecto Fin de Carrera, Seguridad en vehículos de dos ruedas.
 * Codigo Android encargado de la comunicación BT con Arduino y
 * la impletación de un algoritmo para detectar un posible accidente.
 * @author: Valentín Sánchez Ramírez
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.R.string;
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

   
  	//Inicialización de las varialbes correspondientes a la comunicación Bluetooth
  	BluetoothAdapter bluetooth;
  	Handler puente;
  	BluetoothSocket clientSocket;
  	BluetoothDevice myDevice = null;
  	
  	//UUIID que usaremos para el Bluetooth
  	UUID uuid= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
	//TextView´s para visualizar como se comporta el algoritmo y comprobar su estado
	private TextView lblLatitud;
	private TextView lblLongitud;
	private TextView lblPrecision;
	private TextView lblEstado;
	
	//Inicialización de las varialbes correspondientes al GPS
	private LocationManager locManager;
	private LocationListener locListener;
	
	//boton encendido y apagado de la aplicación
	Button buttonOn;
	Button buttonOff;
	
  
 
@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    setContentView(R.layout.activity_main);
    
	lblLatitud = (TextView) findViewById(R.id.LblPosLatitud);
	lblLongitud = (TextView) findViewById(R.id.LblPosLongitud);
	lblPrecision = (TextView) findViewById(R.id.LblPosPrecision);
	lblEstado = (TextView) findViewById(R.id.LblEstado);
	//inicializo button y fijamos la accion que queremos que ejecute
	buttonOn = (Button)findViewById(R.id.buttonOn);
	buttonOff = (Button)findViewById(R.id.buttonOff);
	
	//Método encargado del encendido de la app
	buttonOn.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//get Bluetooth adapter 
		    bluetooth = BluetoothAdapter.getDefaultAdapter();		
		    //Comprobamos si el BT esta encendido o apagado
		    checkBTState(); 
		    //Comprobamos si el GPS esta encendido o apagado
		    checkGpsState() ;
		    
		    //Inicializacion del Handler
		    inicioHandler();
		    Log.d("OnCreate", "inicializo todo");	
		}
	});
    
	//Metodo encargado del apagado de la app
		buttonOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
    
    
    
    
    
  }
  
  
////////////////////////////////////////////////////////////////////////////////////////////////////////////	
//Incializar Handler,
//permite la lectura de los datos almacenados en el buffer, para posteriormente enviarlos 
//como una cadena String al metodo operoDatos()
////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void inicioHandler(){
	  puente = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				byte[] buffer   = null;
			    String mensaje  = null;
			            buffer = (byte[])msg.obj;
			            //converte a String todo lo leido por buffer.
			            mensaje = new String(buffer, 0, msg.arg1);
			            Log.d("mensaje que recibimos:",""+mensaje);
			            operoDatos(mensaje);   
			}
		};
  }
  
////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo operoDatos(), encargado de recibir un String y convertir los números separados
//por las letras "a", "b", "c" y "d" en datos tipo double, para posteriormente ser evaluados
//en nuestro algoritmo  
////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void operoDatos(String leido){

	  int pos1 = leido.indexOf("a");
	  int pos2 = leido.indexOf("b", pos1);
	  int pos3 = leido.indexOf("c", pos2);
	  int pos4 = leido.indexOf("d", pos3);
	  //conversion de String a double en caso de existir en el mismo String todas la variables buscadas
	  if(pos1 !=-1 && pos2 !=-1 && pos3!=-1 && pos4!=-1){
		  double x = Double.valueOf(leido.substring(pos1+1, pos2));
		  double y = Double.valueOf(leido.substring(pos2+1, pos3));
		  double z = Double.valueOf(leido.substring(pos3+1, pos4));

		  algoritmoEmergencia(x, y, z);
	  }
  }
  
////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo encargado de calcular el ángulo de inclinación de la moto a partir de los datos recibidos
//anteriormente y procesados. Comprueba si el angulo de inclinación "theta" es superior a 55 grados en
//ambos lados, en caso de ser afirmativo procedemos a comprobar la velocidad a la que cirulamos.  
////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private void algoritmoEmergencia(double x, double y, double z){
	  double theta;
	  double phi;
	  //constante para que pi sea 180 grados
	  double ang = (180/3.1415);
	  //calculo del angulo theta
	  theta = Math.atan2(x, z)*ang;
	  phi = Math.atan2(y, z);
	  lblLatitud.setText("theta:"+theta);
	  lblLongitud.setText("no me he caido");
	  if(theta>55 || theta<-55){
		  checkSpeed();
		  lblLongitud.setText("es posible que me haya caido");
	  }
  }
  
////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo encargado de calcular la velocidad a la que circulamos, para ello inicializamos
//el GPS, el cual nos devolvera la velocidad a la que circulamos. En caso de ser inferior a 
//3 metros por segundo procedemos a que el GPS nos devuelva nuestra latitud, longitud y
//precisión del mismo, para posteriormente llamar al método sendSMS().  
////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void checkSpeed(){
	  lblPrecision.setText("mido velocidad");
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
				float speed = location.getSpeed();
				if(speed < 3){
					lblPrecision.setText("me he caido :(");
					//las variables que devuelve el GPS con la ubicacion son de tipo double
					double latitud = location.getLatitude();
					double longitud = location.getLongitude();
					double precision = location.getAccuracy();
					sendSMS("000","Me encuentro en", latitud, longitud, precision);
				}
			}

			@Override
			public void onProviderDisabled(String arg0) {	
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		};
		
		locManager.requestLocationUpdates(
		LocationManager.GPS_PROVIDER, 30, 0, locListener);
  }
  
 
  
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo encargado de comprobar el estado del Bluetooth y si nuestro dispositivo posee tal.
//Comprobamos los dispositivos que capta nuestro Bluetooth y si alguno se correcpode con nuestro
//HC-05 lo asociamos, para posteriormente conectarlo y establecer la comunicación. Ademas inicializamos
//la recepción de datos.  
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
			toastText = "Bluetooth disabled, connect it";
			finish();
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
			Toast.makeText(this, "No paired device found", Toast.LENGTH_SHORT).show();
			finish();
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
			e.printStackTrace();
		}
		Toast.makeText(this, "Conectado!", Toast.LENGTH_SHORT).show();
	}
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////	
  //Comprobamos el estado GPS, en caso de estar apagado procedemos a encenderlo
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
  //metodo encargado de la visualización del mensaje de alerta para encender Bluetooth en nuestro dispositivo,
  //en caso afirmativo podremos activarlo, en caso contrario la app se cerrara automaticamente.
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
//Metodo envio SMS, el cual envia un SMS al numero de emergias indicándole nuestra posición.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
int numMensajes = 0;

private void sendSMS (String phoneNumber, String message, double latitud, double longitud, double precision){
	SmsManager sms = SmsManager.getDefault();
	String latitudS = Double.toString(latitud);
	String longitudS = Double.toString(longitud);
	String precisionS = Double.toString(precision);
	message = "he sufrido un accidente, me encuentro en latitud:"+latitudS+"longitud:"+longitudS+"precision:"+precisionS;
	//la varaible numMensajes será la encargada de que solo se envie un 1 SMS y no varios
	lblEstado.setText(message);
	//TODO añadir numero al que enviar mensaje "phoneNumber"
	if(numMensajes !=1){
		sms.sendTextMessage(phoneNumber, null, message, null, null);
		numMensajes=1;
	}
	
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  
  
	@Override
	protected void onStop() {
		super.onStop();
	}
  

	@Override
	public void onResume() {
		super.onResume();
	}
 
	@Override
	public void onPause() {
		super.onPause();   
	}
   
  

  ////////////////////////////////////////////////////////////////////////////////////////
  //Metodo encargado del envio de datos a la placa Arduino
  ////////////////////////////////////////////////////////////////////////////////////////
	public void enviarComando(String comando){
		DataOutputStream outputChannel;
		try {
			outputChannel = new DataOutputStream(clientSocket.getOutputStream());
			outputChannel.writeChars(comando);
			outputChannel.flush();
			Log.i("enviando comando", comando);
		} catch (IOException e) {
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
	    MainActivity mainActivity = new MainActivity();
		
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
			        Log.e("Bluetooth", "HiloConexion(): Error al obtener flujos de E/S", e);
			    }
			        inputStream = tmpInputStream;
			        outputStream = tmpOutputStream;
			}
		
		
		//Metodo principal del hilo, encargado de realizar las lecturas
		int numPerdidas=0;
		int estado = 0;
		public void run()
		{
			byte[] buffer = new byte[1024];
			int bytes;

			// Mientras se mantenga la conexion el hilo se mantiene en espera ocupada
			// leyendo del flujo de entrada
			while(true)
			{
				try {
					if (estado ==1){
						numPerdidas++;
					}
					if(numPerdidas==5){
						mainActivity.checkSpeed();
					}
					
					Thread.sleep(600);
					// Leemos del flujo de entrada del socket
					bytes = inputStream.read(buffer);
					// Enviamos la informacion a la actividad a traves del handler.
					// El metodo handleMessage sera el encargado de recibir el mensaje
					// y operar con ellos tal y como le indiquemos
					puente.obtainMessage(1, bytes, -1, buffer).sendToTarget();
				}
				catch(IOException e) {
					estado = 1;
					Log.e("Bluetooth", "HiloConexion.run(): Error al realizar la lectura", e);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

//Fin
}
		
		
	
 
