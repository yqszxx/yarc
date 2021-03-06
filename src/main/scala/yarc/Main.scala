// See README.md for license details.

package yarc

/**
  * runMain yarc.Main
  */
object Main extends App {
  import chisel3.iotesters._

  class Tester(c: DataPath) extends PeekPokeTester(c) {
    reset(10)
    while (peek(c.io.done) != 1) {
      step(1)
    }
  }

  Driver.execute(args, () => new DataPath()) {
    c => new Tester(c)
  }

//  Driver.execute(args, () => new DataPath)
}
