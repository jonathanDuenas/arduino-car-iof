#include <SoftwareSerial.h>
#include <Wire.h>

#define E1 10  // Enable Pin for motor 1
#define E2 11  // Enable Pin for motor 2
 
#define I1 8  // Control pin 1 for motor 1
#define I2 9  // Control pin 2 for motor 1
#define I3 12  // Control pin 1 for motor 2
#define I4 13  // Control pin 2 for motor 2

const int trigPin = 4;
const int echoPin = 3;

int ledF = A3; // led pinds
long duration;
int distance;

int tempPin = A1;   // the output pin of LM35
float temp;

int valores[5];
int cont =0;

const int MPU_addr=0x68;  // I2C address of the MPU-6050
int16_t AcX,AcY,AcZ,Tmp,GyX,GyY,GyZ; // declare accellerometer and gyro variables

// Initializing communication ports
SoftwareSerial mySerial(6, 5); // TX/RX pins
 
void setup()  
{
  pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
  pinMode(echoPin, INPUT); // Sets the echoPin as an Input
  pinMode(tempPin, INPUT);
  
  pinMode(ledF, OUTPUT);
  pinMode(E1, OUTPUT);
  pinMode(E2, OUTPUT);
 
  pinMode(I1, OUTPUT);
  pinMode(I2, OUTPUT);
  pinMode(I3, OUTPUT);
  pinMode(I4, OUTPUT);
  
  Wire.begin(); // initiate i2c system
  Wire.beginTransmission(MPU_addr); // be sure we talk to our MPU vs some other device
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true); // done talking over to MPU device, for the moment
  
  Serial.begin(9600);
  mySerial.begin(9600);
}
 
String getMessage(){
  String msg = "";
  char a;
  
  while(mySerial.available()) {
      a = mySerial.read();
      msg+=String(a);
  }
  return msg;
}
 
boolean adelante = true;
boolean stopB = true;

boolean led = false;

void loop()
{
    // Check if a message has been received
    String msg = getMessage();
    if(msg!=""){
      Serial.println(msg);
      if(msg == "a"){
        adelante = true;
        stopB = false;
      }
      if(msg == "r"){
        adelante = false;
        stopB = false;
      }
      if(msg == "s"){
        stopB = true;
      }
      if(msg == "e"){
        led = true;
      }
      if(msg == "o"){
        led = false;
      }
    }
    
    if(led){
      analogWrite(ledF, 255);
    }else{
      analogWrite(ledF, 0);
    }
    
    Serial.flush();
    
    if(!stopB){
      if(adelante){
       analogWrite(E1, 100);
       analogWrite(E2, 100);
 
       digitalWrite(I1, HIGH);
       digitalWrite(I2, LOW);
       digitalWrite(I3, HIGH);
       digitalWrite(I4, LOW);  
      
      }else{
        analogWrite(E1, 100);
        analogWrite(E2, 100);
        
        digitalWrite(I1, LOW);
        digitalWrite(I2, HIGH);
        digitalWrite(I3, LOW);
        digitalWrite(I4, HIGH);
      }
      
    }else{
       digitalWrite(E1, LOW);
       digitalWrite(E2, LOW);
      
       digitalWrite(I1, LOW);
       digitalWrite(I2, LOW);
       digitalWrite(I3, LOW);
       digitalWrite(I4, LOW);
    
    }
    
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);

    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);

    duration = pulseIn(echoPin, HIGH);

    distance= duration*0.034/2;
    
    valores[cont] = distance;
    
    if(distance <= 17)
      cont++;
    else
      cont =0;
    
    mySerial.print("D:");
    mySerial.println(distance);
    
    mySerial.print("T:");
    mySerial.println(readTemp());
    
    if(cont == 7 && adelante && stopB==false){
   
        digitalWrite(E1, LOW);
        digitalWrite(E2, LOW);
      
        analogWrite(E1, 100);
        analogWrite(E2, 100);
        
        digitalWrite(I1, LOW);
        digitalWrite(I2, HIGH);
        digitalWrite(I3, LOW);
        digitalWrite(I4, HIGH);
       
        delay(500);
        stopB = true;
        cont = 0;
      
    }
   
    md6050();
   Serial. 
    delay(10);
}

void md6050(){
  Wire.beginTransmission(MPU_addr); // get ready to talk to MPU again
  Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false); // done talking to MPU for the time being
  Wire.requestFrom(MPU_addr,14,true);  // request a total of 14 registers
  // all the fancy <<8| stuff is to bit shift the first 8 bits to
  // the left & combine it with the next 8 bits to form 16 bits
  AcX=Wire.read()<<8|Wire.read();  // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)    
  AcY=Wire.read()<<8|Wire.read();  // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
  AcZ=Wire.read()<<8|Wire.read();  // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
  Tmp=Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
  GyX=Wire.read()<<8|Wire.read();  // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
  GyY=Wire.read()<<8|Wire.read();  // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
  GyZ=Wire.read()<<8|Wire.read();  // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
  // the above lines have gathered Accellerometer values for X, Y, Z
  //  as well as Gyroscope values for X, Y, Z
  
  
  mySerial.print("AcX = "); mySerial.println(AcX / 16384.0); // share accellerometer values over debug channel 
  mySerial.print("AcY = "); mySerial.println(AcY / 16384.0);
  mySerial.print("AcZ = "); mySerial.println(AcZ / 16384.0);
  mySerial.print("Tmp = "); mySerial.println(Tmp/340.00+36.53);  //equation for temperature in degrees C from datasheet
  mySerial.print("GyX = "); mySerial.println(GyX /131.0); // share gyroscope values over debug channel
  mySerial.print("GyY = "); mySerial.println(GyY / 131.0);
  mySerial.print("GyZ = "); mySerial.println(GyZ / 131.0);
}


float readTemp() {  
  
  int reading = analogRead(tempPin);

   float voltage = reading * 500.0;
   voltage /= 1024.0; 

  float temperatureC = (voltage) ; 
  float temperatureF = (temperatureC * 9.0 / 5.0) + 32.0;
  
  return temperatureC;
}
