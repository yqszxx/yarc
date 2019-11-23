package uart

trait UARTParameters {
  val clockFrequency = 12000000 // Hz
  val baudRate = 9600 // bps

  val clockPerBit = (clockFrequency.toDouble / baudRate.toDouble).round
}
