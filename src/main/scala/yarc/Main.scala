// See README.md for license details.

package yarc

import chisel3._

/**
  * runMain yarc.Main
  */
object Main extends App {
  Driver.execute(args, () => new System)
}
