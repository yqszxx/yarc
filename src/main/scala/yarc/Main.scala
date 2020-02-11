// See README.md for license details.

package yarc

import chisel3._

/**
 * Entry point for verilog generation.
 *
 * usage: runMain yarc.Main [args...]
 */
object Main extends App {
  Driver.execute(args, () => new DataPath)
}
