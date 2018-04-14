#pragma once

#include "concurrentqueue.h"
#include "Receiver.h"

#include <inttypes.h>
#include <pthread.h>

#include <array>
#include <atomic>

class Decoder
{
  public:
    using PulseDurationType = decltype(timespec::tv_sec);
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
    pthread_rwlock_t mFetchNewDataLock;

    volatile bool mReceivedNewData;
    struct ReceivedData {
      struct RSSI {
        double value;
        uint32_t count;
      } rssi;
      std::array<uint8_t, DATA_BUFFER_LENGTH> data;
    } mReceivedData;
    static void* decode(void* parameter);

    pthread_t mDecoderThread;
    std::atomic<bool> mStopDecoderThread;
    std::atomic<bool> mDecoderThreadIsAlive;

    moodycamel::ConcurrentQueue<PulseDurationType> mPulseData;
    static void* receive(void* parameter);

    pthread_t mReceiverThread;
    std::atomic<bool> mStopReceiverThread;
    std::atomic<bool> mReceiverThreadIsAlive;
};
