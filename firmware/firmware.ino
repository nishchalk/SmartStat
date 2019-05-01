#include <Wire.h>
#include <Adafruit_MCP4725.h>
#include <SoftwareSerial.h>
#define rxPin 2
#define txPin 3

SoftwareSerial mySerial(rxPin, txPin); // RX, TX

Adafruit_MCP4725 dac;

void setup(void) {
  pinMode(rxPin, INPUT);  
  pinMode(txPin, OUTPUT);  
  mySerial.begin(9600);

  Serial.begin(9600);
  Serial.println("SmartStat!");
  dac.begin(0x60);
}

void loop(void) { 
    int dacValue, adcValue = 0;
    int cycle, numCycles = 0;
    
    float adcVoltage, increment;
    int mdelay = 99;
    int eqTime;
    float scanRate;
    boolean debug = true;

    if(mySerial.available() && mySerial.parseInt() == 1 ){
      mySerial.println("Smartstat initialized");
      delay(1000);
      
      while(mySerial.available() == 0){}
      scanRate = mySerial.parseInt();
      scanRate = scanRate * 0.002;
      if(scanRate <= 0.00){
        scanRate = 0.01;
      }
      
      while(mySerial.available() == 0){}
      numCycles = mySerial.parseInt();
      if(numCycles <= 0){
        numCycles = 1;
      }
      
      while(mySerial.available() == 0){}
      eqTime = mySerial.parseInt();
      if(eqTime <= 0){
        eqTime = 0;
      }

      mySerial.print("scanRate: ");
      mySerial.print(scanRate);
      mySerial.print("V/s");
      mySerial.print("\tnumber of Cycles: ");
      mySerial.print(numCycles);
      mySerial.print("\t eqTime: ");
      mySerial.print(eqTime);
      mySerial.println("s");

      increment = (int)((scanRate * 819 * 0.099) + 0.5); //increment calculated to match scanRate to actual scan rate
      eqTime = eqTime * 1000;
      
      if(debug){
        Serial.println(scanRate);
        Serial.println(eqTime);
        Serial.println(increment);
      }
      
      delay(eqTime);
      
      for(cycle = 0; cycle < numCycles; numCycles--){
        // Run through the full 12-bit scale for a triangle wave
        //start of increasing for loop
        for (dacValue = 0; dacValue < 4096 ; dacValue = dacValue + increment)
        {
          dac.setVoltage(dacValue, false);
          delay(mdelay);
          adcValue = analogRead(A1);
          mySerial.println(adcValue);
          if(debug){
            Serial.print("dacValue: ");
            Serial.print(dacValue);
            Serial.print("\tADC Value: ");
            Serial.print(adcValue);
            Serial.print("\tVoltage: ");
            adcVoltage = (adcValue*5)/1023.0;
            Serial.println(adcVoltage, 3); 
          }
        }
        //end of increasing for loop
        //start of decreasing for loop
        for (dacValue = dacValue - 2*increment; dacValue >= 0 ; dacValue = dacValue - increment)
        {
          dac.setVoltage(dacValue, false);
          delay(mdelay);
          adcValue = analogRead(A1);
          mySerial.println(adcValue);
          if(debug){
            Serial.print("dacValue: ");
            Serial.print(dacValue);
            Serial.print("\tADC Value: ");
            Serial.print(adcValue);
            Serial.print("\tVoltage: ");
            adcVoltage = (adcValue*5)/1023.0;
            Serial.println(adcVoltage, 3); 
          }
          if(dacValue - increment != 0){
            dac.setVoltage(0, false);
          }
        }
        //end of decreasing for loop 
      }
      //end of cycles for loop
      mySerial.println("Analysis Complete");
    }
    //end of if condition
}
