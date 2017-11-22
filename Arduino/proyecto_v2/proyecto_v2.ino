#include <QueueList.h>
#include <SoftwareSerial.h>
#include "header.h"
#include <avr/wdt.h>

QueueList <char> queue;
int valores[5];
int cont =0;
int err = 0;

int PINtoRESET = 7;
// Initializing communication ports
SoftwareSerial mySerial(6, 5); // TX/RX pins

void setup(){
  initEngine();
  initMd6050();
  initMisc();
  
  wdt_enable(WDTO_1S);

  Serial.begin(9600);
  mySerial.begin(9600);
}

boolean adelante = true;
boolean stopB = true;
boolean led = false;
int distance;

void loop(){
  
  String val = getMessage();
  
  enqueeMsg(val);
  
  Serial.flush();
  mySerial.flush();
  
  if(val == "")
    err++;
  else
    err =0;
  
  while(!queue.isEmpty()){
    char val = queue.pop();
    Serial.println("VAL: " + val);
    if(val == 'a'){
      adelante = true;
      stopB = false;
    }
    if(val == 'r'){
      adelante = false;
      stopB = false;
    }
    if(val == 's'){
      stopB = true;
    }
    if(val == 'e'){
      led = true;
    }
    if(val == 'o'){
      led = false;
    }
  }

  if(led){
    analogWrite(ledF, 255);
  }
  else{
    analogWrite(ledF, 0);
  }

  if(!stopB){
    if(adelante){
      drive(120);
    }
    else{
      reverse(120);
    }
  }
  else{
    halt();
  }

  distance = readDistance();
  valores[cont] = distance;

  if(distance <= 17)
    cont++;
  else
    cont =0;

  if(cont == 3 && adelante && stopB==false){ 
    halt();
    reverse(120);
    delay(1000);

    stopB = true;
    cont = 0; 
  }
  
  md6050();
  
  if(err <5)
    wdt_reset();
     
  delay(333);
}




