#pragma once

#include "Receiver.h"

class RXB final : public Receiver {
  public:
    RXB();
    virtual ~RXB();

    State isInitialized() const override;
    double getRSSIValue() const override;

  private:
    RXB(const RXB& other) = delete;
    RXB& operator=(const RXB& other) = delete;
};
