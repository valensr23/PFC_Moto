#include <L3G.h>
#include <LSM303.h>
#include <Wire.h>
#include <SoftwareSerial.h> //Librería que permite establecer comunicación serie en otros pins

L3G gyro;
LSM303 compass;

//inicializo variables que usare.
//inicializar valores de orientacion signvector
//comunicacion BT con serial y write en TX
int AN[5];
int SENSOR_SIGN[8];
int AN_OFFSET[5];
int gyro_x;
int gyro_y;
int gyro_z;
int accel_x;
int accel_y;
int accel_z;
int magnetom_x;
int magnetom_y;
int magnetom_z;




//Aquí conectamos los pins RXD,TDX del módulo Bluetooth.
SoftwareSerial BT(10,11); //10 RX, 11 TX.



void I2C_Init()
{
  Wire.begin();
}

void Gyro_Init()
{
  gyro.init();
  //gyro.writeReg(L3G_CTRL_REG4, 0x20); // 2000 dps full scale
  //gyro.writeReg(L3G_CTRL_REG1, 0x0F); // normal power mode, all axes enabled, 100 Hz
}

void Read_Gyro()
{
  gyro.read();
  
  AN[0] = gyro.g.x;
  AN[1] = gyro.g.y;
  AN[2] = gyro.g.z;
  gyro_x = SENSOR_SIGN[0] * (AN[0] - AN_OFFSET[0]);
  gyro_y = SENSOR_SIGN[1] * (AN[1] - AN_OFFSET[1]);
  gyro_z = SENSOR_SIGN[2] * (AN[2] - AN_OFFSET[2]);
}

void Accel_Init()
{
  compass.init();
  compass.enableDefault();
  switch (compass.getDeviceType())
  {
    case LSM303::device_D:
      compass.writeReg(LSM303::CTRL2, 0x18); // 8 g full scale: AFS = 011
      break;
    case LSM303::device_DLHC:
      compass.writeReg(LSM303::CTRL_REG4_A, 0x28); // 8 g full scale: FS = 10; high resolution output mode
      break;
    default: // DLM, DLH
      compass.writeReg(LSM303::CTRL_REG4_A, 0x30); // 8 g full scale: FS = 11
  }
}

// Reads x,y and z accelerometer registers
void Read_Accel()
{
  compass.readAcc();
  
  AN[3] = compass.a.x >> 4; // shift left 4 bits to use 12-bit representation (1 g = 256)
  AN[4] = compass.a.y >> 4;
  AN[5] = compass.a.z >> 4;
  accel_x = SENSOR_SIGN[3] * (AN[3] - AN_OFFSET[3]);
  accel_y = SENSOR_SIGN[4] * (AN[4] - AN_OFFSET[4]);
  accel_z = SENSOR_SIGN[5] * (AN[5] - AN_OFFSET[5]);
}

void Compass_Init()
{
  // doesn't need to do anything because Accel_Init() should have already called compass.enableDefault()
}

void Read_Compass()
{
  compass.readMag();
  
  magnetom_x = SENSOR_SIGN[6] * compass.m.x;
  magnetom_y = SENSOR_SIGN[7] * compass.m.y;
  magnetom_z = SENSOR_SIGN[8] * compass.m.z;
}



void setup()
{
  //BT.begin(9600); //Velocidad del puerto del módulo Bluetooth
  Serial.begin(9600); //Abrimos la comunicación serie con el PC y establecemos velocidad
  //Wire.begin(0xF5); //Inicializo el bus I2C en la direccion 0xF5
  I2C_Init();
  gyro.init();
  compass.init();
  compass.enableDefault();
}


 
void loop()
{
  Read_Gyro();
  compass.read();
  //modulo de prueba para comunicacion con el ordenador
  int datos[] = {gyro.g.x, gyro.g.y, gyro.g.z, compass.a.x,  compass.a.y,  compass.a.z, compass.m.x, compass.m.y, compass.m.z};
  for (int i=0;i<=8;i++){
  Serial.println(datos[i]);
  }
  delay(5000);
 
/* Comunicacion Android to Arduino
  if(BT.available())
  {
    
    Serial.write(BT.read());
  }
 */
 
 //Comunicacion BT to Android
 /*
  while(Serial.available())
  {
     char datos[] = {gyro_x, gyro_y, gyro_z, accel_x, accel_y, accel_z, magnetom_x, magnetom_y, magnetom_z};
     BT.write(Serial.read());
     BT.print(datos);
     delay(1000);
  }*/
}
