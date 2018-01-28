#include "RXB.h"

RXB::RXB()
  :Receiver()
{
}

RXB::~RXB()
{
}

RXB::State RXB::isInitialized() const
{
  return State::INITIALIZED;
}

double RXB::getRSSIValue() const
{
  return 0.0;
}
