// See README.md for license details.

package yarc

import chisel3._
import net.fornwall.jelf._
import java.io.File

/**
  * runMain yarc.Main
  */
object Main extends App {
//  val elfFile = ElfFile.fromFile(new File("testelf"))
//  println(s"SIZE = ${elfFile}")

//  import chisel3.iotesters._
//
//  class Tester(c: DataPath) extends PeekPokeTester(c) {
//    reset(10)
//    var cycle = 0
//    while (peek(c.io.done) != 1 && cycle < 32) {
//      println(s"Cycle $cycle:")
//      step(1)
//      cycle += 1
//    }
//  }
//
//  Driver.execute(args, () => new DataPath()) {
//    c => new Tester(c)
//  }



  Driver.execute(args, () => new DataPath)
}
