/*This is the sample program to notify us for the file creation and file deletion takes place in “/tmp” directory*/
#include "Decoder.h"
#include "CC1101.h"

#include <unistd.h>
#include <iostream>

int main( )
{
  CC1101 receiver("/dev/spidev0.1", 0);
  Decoder decoder(21, receiver);
  decoder.start();

  while(true) {
    double rssi = 0.0;
    std::array<uint8_t, Decoder::DATA_BUFFER_LENGTH> data;
    int length = decoder.getDecodedData(data, rssi);
    if(length > 0) {
      std::cout << "Got new " << length << " data bytes with RSSI: " << rssi << std::endl;
    }
    sleep(1);
  }
}