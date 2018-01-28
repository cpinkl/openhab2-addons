#pragma once

#include <inttypes.h>

class Receiver {
  public:
    enum class State : uint8_t {
      ERROR,
      INITIALIZED
    };

    Receiver();
    virtual ~Receiver();

    virtual State isInitialized() const = 0;
    virtual double getRSSIValue() const = 0;

  private:
    Receiver(const Receiver& other) = delete;
    Receiver& operator=(const Receiver& other) = delete;
};
