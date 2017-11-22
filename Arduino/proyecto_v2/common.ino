#include <Wire.h>

int16_t AcX,AcY,AcZ,Tmp,GyX,GyY,GyZ; // declare accellerometer and gyro variables

/*

 Init functions
 
 */

void initEngine(){
  pinMode(E1, OUTPUT);
  pinMode(E2, OUTPUT);

  pinMode(I1, OUTPUT);
  pinMode(I2, OUTPUT);
  pinMode(I3, OUTPUT);
  pinMode(I4, OUTPUT);
}

void initMd6050(){
  Wire.begin(); // initiate i2c system
  Wire.beginTransmission(MPU_addr); // be sure we talk to our MPU vs some other device
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true); // done talking over to MPU device, for the moment
}

int tempPin = A1;   // the output pin of LM35
void initMisc(){
  pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
  pinMode(echoPin, INPUT); // Sets the echoPin as an Input
  pinMode(tempPin, INPUT);
  pinMode(ledF, OUTPUT);
}

/*

 Sensors data functions
 
 */

void md6050(){

  Wire.beginTransmission(MPU_addr); // get ready to talk to MPU again
  Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(0); // done talking to MPU for the time being
  Wire.requestFrom(MPU_addr,14,1);  // request a total of 14 registers
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

  mySerial.print("AcX = "); 
  mySerial.println(AcX ); // share accellerometer values over debug channel 
  mySerial.print("AcY = "); 
  mySerial.println(AcY );
  mySerial.print("AcZ = "); 
  mySerial.println(AcZ );
  mySerial.print("T:"); 
  mySerial.println(Tmp/340.00+36.53);  //equation for temperature in degrees C from datasheet
  mySerial.print("GyX = "); 
  mySerial.println(GyX ); // share gyroscope values over debug channel
  mySerial.print("GyY = "); 
  mySerial.println(GyY );
  mySerial.print("GyZ = "); 
  mySerial.println(GyZ );
}

float readTemp() {

  int reading = analogRead(tempPin);

  float voltage = reading * 500.0;
  voltage /= 1024.0; 

  float temperatureC = (voltage) ; 
  float temperatureF = (temperatureC * 9.0 / 5.0) + 32.0;

  mySerial.print("T:");
  mySerial.println(temperatureC);

  return temperatureC;
}

int readDistance(){
  long duration;
  int distance;

  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);

  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  duration = pulseIn(echoPin, HIGH);

  distance= duration*0.034/2;

  mySerial.print("D:");
  mySerial.println(distance);

  return distance;
}

/*
  
 Engines
 
 */

void halt(){
  digitalWrite(E1, LOW);
  digitalWrite(E2, LOW);

  digitalWrite(I1, LOW);
  digitalWrite(I2, LOW);
  digitalWrite(I3, LOW);
  digitalWrite(I4, LOW);
}

void drive(int pow){
  analogWrite(E1, pow);
  analogWrite(E2, pow);

  digitalWrite(I1, HIGH);
  digitalWrite(I2, LOW);
  digitalWrite(I3, HIGH);
  digitalWrite(I4, LOW); 
}

void reverse(int pow){
  analogWrite(E1, pow);
  analogWrite(E2, pow);

  digitalWrite(I1, LOW);
  digitalWrite(I2, HIGH);
  digitalWrite(I3, LOW);
  digitalWrite(I4, HIGH);
}

/*
  
 Misc
 
 */

String getMessage(){
  String msg = "";
  char a;

  while(mySerial.available()) {
    a = mySerial.read();
    msg+=String(a);
  }
  return msg;
}

String split(String data, char separator, int index){
  int found = 0;
  int strIndex[] = { 
    0, -1       };
  int maxIndex = data.length() - 1;

  for (int i = 0; i <= maxIndex && found <= index; i++) {
    if (data.charAt(i) == separator || i == maxIndex) {
      found++;
      strIndex[0] = strIndex[1] + 1;
      strIndex[1] = (i == maxIndex) ? i+1 : i;
    }
  }
  return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}



String codes[] = {
  "a", "s", "r", "e", "o"};

String old = "";

void enqueeMsg(String msg){
  
  msg.trim();
  
  Serial.print("MS:"+ msg + "SZ:");
  Serial.println(msg.length());
  if(msg != old && msg.length() == 4 && msg.charAt(0) == '&' && msg.charAt(3) == '&'){
    Serial.println("Entra");
    for(int i = 0; i<4;i++){
       queue.push(msg.charAt(i));
    }
    old = msg;
  }
}



