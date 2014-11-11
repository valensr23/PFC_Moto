package motosafe.app;

/*
 * @author: Valentín Sánchez Ramírez
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.*;


	
import java.util.Set;
 


public class MainActivity extends Activity  {

	private static final String TAG = "InicioActivity";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final String NOMBRE_DISPOSITIVO_BT = "HC-06";//Nombre de nuestro dispositivo bluetooth.
	private TextView tvInformacion;
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
	protected void onCreate(Bundle savedInstanceState) {
	/*Inicializamos la activity e inflamos el layout*/
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	tvInformacion = (TextView) findViewById(R.id.textView_estado_BT);
	lblLatitud = (TextView) findViewById(R.id.LblPosLatitud);
	lblLongitud = (TextView) findViewById(R.id.LblPosLongitud);
	lblPrecision = (TextView) findViewById(R.id.LblPosPrecision);
	lblEstado = (TextView) findViewById(R.id.LblEstado);
	//inicializo button y fijamos la accion que queremos que desarrolle
	buttonSMS = (Button)findViewById(R.id.buttonSMS);
	buttonSMS.setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			sendSMS("000","Mi posicion es latitud:" );
			
		}
	});
	

	//Comprobamos si el GPS esta encendido o apagado
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
	        Toast.makeText(this, "GPS is Enabled in your devide", Toast.LENGTH_SHORT).show();
	    }else{
	        showGPSDisabledAlertToUser();
	    }
    
    
	}



@Override
	protected void onResume() {
	/* El metodo on resume es el adecuado para inicialzar todos aquellos procesos que actualicen la interfaz de usuario
	Por lo tanto invocamos aqui al método que activa el BT y GPS*/
	super.onResume();
	descubrirDispositivosBT();
	comenzarLocalizacion();
	}


////////////////////////////////////////////////////////////////////////////////////////////////
//Metodo para comprobar el estado del GPS y en caso de estar desactivado activarlo.
////////////////////////////////////////////////////////////////////////////////////////////////

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
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
		//Salimos de la app
		finish();
		}

    }).show();
}




////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Vemos si hay dispositivos emparejados al BT
///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void descubrirDispositivosBT() {

	/*
	Este método comprueba si nuestro dispositivo dispone de conectividad bluetooh.
	En caso afirmativo, si estuviera desctivada, intenta activarla.
	En caso negativo sale de la aplicación.
	*/
	
	//Comprobamos que el dispositivo tiene adaptador bluetooth
	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	tvInformacion.setText("Comprobando bluetooth");
	if (mBluetoothAdapter != null) {
		//El dispositivo tiene adapatador BT. Ahora comprobamos que bt esta activado.
		if (mBluetoothAdapter.isEnabled()) {
				//Esta activado. Obtenemos la lista de dispositivos BT emparejados con nuestro dispositivo android.
				tvInformacion.setText("Obteniendo dispositivos emparejados, espere...");
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				//Si hay dispositivos emparejados
			if (pairedDevices.size() > 0) {
						/*
						Recorremos los dispositivos emparejados hasta encontrar el
						adaptador BT del arduino, en este caso se llama HC-06
						*/
					
					
					BluetoothDevice arduino = null;
					for (BluetoothDevice device : pairedDevices) {
						if (device.getName().equalsIgnoreCase(NOMBRE_DISPOSITIVO_BT)) {
						arduino = device;
						}
					}
					if (arduino != null) {
						//TODO las operasciones que queramos al estar ya conectado
					} else {
					//No hemos encontrado nuestro dispositivo BT, es necesario emparejarlo antes de poder usarlo.
					//No hay ningun dispositivo emparejado. Salimos de la app.
					Toast.makeText(this, "No hay dispositivos emparejados, por favor, empareje el arduino", Toast.LENGTH_LONG).show();
					//finish();
					}
				} else {
				
				//No hay ningun dispositivo emparejado. Salimos de la app.
				Toast.makeText(this, "No hay dispositivos emparejados, por favor, empareje el arduino", Toast.LENGTH_LONG).show();
				//finish();
				}
			} else {
			muestraDialogoConfirmacionActivacion();
			}
		} else {
	// El dispositivo no soporta bluetooth. Mensaje al usuario y salimos de la app
	Toast.makeText(this, "El dispositivo no soporta comunicación por Bluetooth", Toast.LENGTH_LONG).show();
	}
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	

@Override
	protected void onStop() {
	
	/*Cuando la actividad es destruida, se ejecuta este método.
	*/
	super.onStop();
	
		
	
	}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Dialogos para la activacion del BT
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

private void muestraDialogoConfirmacionActivacion() {
	new AlertDialog.Builder(this)
	.setIcon(android.R.drawable.ic_dialog_alert)	
	.setTitle("Activar Bluetooth")	
	.setMessage("BT esta desactivado. ¿Desea activarlo?")	
	.setPositiveButton("Si", new DialogInterface.OnClickListener() {
	
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//Intentamos activarlo con el siguiente intent.
			tvInformacion.setText("Activando BT");
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
			
			}).setNegativeButton("No", new DialogInterface.OnClickListener() {
			
				@Override
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



}
