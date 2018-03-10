#pragma once

#include "Receiver.h"

#include <inttypes.h>
#include <pthread.h>

#include <array>

class Decoder
{
  public:
    static constexpr std::size_t DATA_BUFFER_LENGTH = 15;

    Decoder(const int& pin, const Receiver& receiver);
    virtual ~Decoder();

    virtual void setTimeout(const int& timeout);

    virtual bool start();
    virtual bool stop();

    int getDecodedData(std::array<uint8_t, DATA_BUFFER_LENGTH>& data, double& rssi);

  private:
    Decoder() = delete;
    Decoder(const Decoder& other) = delete;
    Decoder& operator=(const Decoder& other) = delete;

    int mPin;
    int mTimeout;
    const Receiver& mReceiver;

    volatile bool mReceivedNewData;
    struct ReceivedData {
      struct RSSI {
        double value;
        uint32_t count;
      } rssi;
      std::array<uint8_t, DATA_BUFFER_LENGTH> data;
    } mReceivedData;
    static void* decode(void* parameter);

    volatile bool mStopDecoderThread;
    bool mDecoderThreadIsAlive;
    pthread_t mDecoderThread;
    pthread_rwlock_t mDecoderLock;
};
