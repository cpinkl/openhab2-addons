#include "Decoder.h"
#include "GPIO.h"

#include <fcntl.h>
#include <math.h>
#include <poll.h>
#include <unistd.h>
#include <sys/stat.h>

#include <cstring>
#include <limits>

Decoder::Decoder(const int& pin, const Receiver& receiver)
  :mPin(pin),
   mTimeout(-1),
   mReceiver(receiver),
   mReceivedNewData(false),
   mStopDecoderThread(false),
   mDecoderThreadIsAlive(false),
   mDecoderLock(PTHREAD_RWLOCK_INITIALIZER)
{
  if(pthread_rwlock_init(&mDecoderLock, nullptr) != 0) {
    mPin = std::numeric_limits<decltype(mPin)>::max();
  }
}


Decoder::~Decoder()
{
  stop();
  if(mPin != std::numeric_limits<decltype(mPin)>::max()) {
    pthread_rwlock_destroy(&mDecoderLock);
    mPin = -1;
  }
}

void Decoder::setTimeout(const int& timeout)
{
  if (!mDecoderThreadIsAlive) {
    mTimeout = timeout;
  }
}

/* Start decoder on pin
* Result: 0 = O.K., -1 = Error
*/
bool Decoder::start() {
  std::memset(&mReceivedData, 0, sizeof(decltype(mReceivedData)));
  if ((0 < mPin) && (mPin < 41) && !mDecoderThreadIsAlive) {
    if (GPIO::enable(mPin, GPIO::Direction::IN, GPIO::Edge::BOTH) == 0) {
      if (pthread_create(&mDecoderThread, nullptr, decode, this) == 0) {
        mDecoderThreadIsAlive = true;
      } else {
        GPIO::disable(mPin);
      }
    }
  }

  return mDecoderThreadIsAlive;
}

/* Stop decoder on pin
* Result: 0 = O.K., -1 = Error
*/
bool Decoder::stop() {
  if (mDecoderThreadIsAlive) {
    mStopDecoderThread = true;
    if (pthread_join(mDecoderThread, nullptr) == 0) {
      mDecoderThreadIsAlive = false;
      GPIO::disable(mPin);
    }
  }

  std::memset(&mReceivedData, 0, sizeof(decltype(mReceivedData)));
  return !mDecoderThreadIsAlive;
}

int Decoder::getDecodedData(std::array<uint8_t, DATA_BUFFER_LENGTH>& data, double& rssi) {
  int length = -1;

  pthread_rwlock_rdlock(&mDecoderLock);
  if (mReceivedNewData) {
    data = mReceivedData.data;
    length = (data[2] >> 1) & 0x1F;
    rssi = mReceivedData.rssi.value / mReceivedData.rssi.count;
    pthread_rwlock_unlock(&mDecoderLock);

    pthread_rwlock_wrlock(&mDecoderLock);
    mReceivedNewData = false;
    std::memset(&mReceivedData, 0, sizeof(decltype(mReceivedData)));
  }
  pthread_rwlock_unlock(&mDecoderLock);

  return length + 1;
}

// Set limits according to
// http://jeelabs.org/2010/04/16/cresta-sensor/index.html
// http://jeelabs.org/2010/04/17/improved-ook-scope/index.html
static constexpr unsigned int LOW_TIME = 183; //200;
static constexpr unsigned int MID_TIME = 726; //750;
static constexpr unsigned int HIGH_TIME = 1464; //1300;
void* Decoder::decode(void* parameter)
{
  Decoder* decoder = reinterpret_cast<Decoder*>(parameter);

  static int count = 0;   // Current bit count
  static int halfBit = 0; // Indicator for received half bit
  static uint32_t value = 0; // Received byte + parity value

  static char file[FILENAME_MAX] = { '\0' }; // Received data
  snprintf(file, FILENAME_MAX, "/sys/class/gpio/gpio%d/value", decoder->mPin);

  // Start decoder
  struct pollfd polldat = { open(file, O_RDONLY), POLLPRI | POLLERR, 0 };
  while (!decoder->mStopDecoderThread && (polldat.fd >= 0)) {
    unsigned int duration = 0;

    if (lseek(polldat.fd, 0, SEEK_SET) >= 0) { // Catch next edge time
      static struct timespec tOld;
      clock_gettime(CLOCK_REALTIME, &tOld);
      int pc = poll(&polldat, 1, decoder->mTimeout);
      static struct timespec tNew;
      clock_gettime(CLOCK_REALTIME, &tNew);

      if ((pc > 0) && (polldat.revents & POLLPRI)) {
        static uint8_t value = 0;
        if (read(polldat.fd, &value, 1) >= 0) {
          duration = round((tNew.tv_nsec - tOld.tv_nsec) / 1000.0); // Pulse length in microseconds
        }
      }
    }

    if (duration < 20) { // Filter pulses shorter than 20 microseconds
      continue;
    }

    int reset = 1;
    // First half bit or one
    if ((MID_TIME <= duration) && (duration < HIGH_TIME)) { // Got 1
      value = value + 1;
      value = value << 1;
      count = count + 1;
      reset = 0;
      halfBit = 0;
    } else if ((LOW_TIME <= duration) && (duration < MID_TIME)) { // Got 0?
      if (halfBit == 1) { // Got 0
        value = value + 0;
        value = value << 1;
        count = count + 1;
      }
      reset = 0;
      halfBit = (halfBit + 1) % 2;
    }

    static uint32_t byte = 0;
    static struct ReceivedData buffer = { .rssi = { 0.0, 0 }, .data = { 0 } };
    std::size_t length = buffer.data.size() + 1;
    if ((byte > 2) && (reset == 0)) {
      length = (buffer.data[2] >> 1) & 0x1F;
      if (length > buffer.data.size() - 1) {
        reset = 1;
      }
    }

    // Last byte has 8 bits only. No parity will be read
    // Fake parity bit to pass next step
    if ((byte == length + 2) && (reset == 0) && (count == 8))
    {
      count = count + 1;
      value = __builtin_parity(value) + (value << 1);
    }

    if ((count == 9) && (reset == 0)) {
      value = value >> 1; // We made one shift more than need. Shift back.
      if (__builtin_parity(value >> 1) == value % 2) {
        buffer.data[byte] = static_cast<decltype(buffer.data)::value_type>((value >> 1) & 0xFF);
        buffer.data[byte] = ((buffer.data[byte] & 0xAA) >> 1) | ((buffer.data[byte] & 0x55) << 1);
        buffer.data[byte] = ((buffer.data[byte] & 0xCC) >> 2) | ((buffer.data[byte] & 0x33) << 2);
        buffer.data[byte] = ((buffer.data[byte] & 0xF0) >> 4) | ((buffer.data[byte] & 0x0F) << 4);

        if (buffer.data[0] == 0x9F) {
          byte = byte + 1;
          buffer.rssi.count += 1;
          buffer.rssi.value += decoder->mReceiver.getRSSIValue();
        } else {
          reset = 1;
        }

        if ((byte > 2) && (reset == 0)) {
          length = (buffer.data[2] >> 1) & 0x1F;
          if (length > buffer.data.size() - 1) {
            reset = 1;
          }
        }

        if ((byte > length + 1) && (reset == 0)) {
          uint32_t crc1 = 0;
          for (uint8_t i = 1; i < length + 1; ++i) {
            crc1 = crc1 ^ buffer.data[i];
          }
          if (crc1 != buffer.data[length + 1]) {
            reset = 1;
          }
        }

        if ((byte > length + 2) && (reset == 0)) {
          uint32_t crc2 = 0;
          for (uint8_t i = 1; i < length + 2; ++i) {
            crc2 = crc2 ^ buffer.data[i];
            for (uint8_t j = 0; j < 8; ++j) {
              if ((crc2 & 0x01) != 0) {
                crc2 = (crc2 >> 1) ^ 0xE0;
              } else {
                crc2 = (crc2 >> 1);
              }
            }
          }

          if (crc2 == buffer.data[length + 2]) {
            pthread_rwlock_wrlock(&decoder->mDecoderLock);
            decoder->mReceivedNewData = true;
            std::memcpy(&decoder->mReceivedData, &buffer, sizeof(decltype(buffer)));
            pthread_rwlock_unlock(&decoder->mDecoderLock);
          }
          reset = 1;
        }
      }
      count = 0;
      value = 0;
      halfBit = 0;
    }

    if (reset == 1) { // Reset if failed or got valid data
      byte = 0;
      count = 0;
      value = 0;
      halfBit = 0;
      std::memset(&buffer, 0, sizeof(decltype(buffer)));
    }
  }

  if (polldat.fd >= 0) {
    close(polldat.fd);
    polldat.fd = -1;
  }

  return nullptr;
}
