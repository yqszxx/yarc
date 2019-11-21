package yarc.elements

import chisel3._

class ROM extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val data = Output(UInt(32.W))
  })

  val rom = VecInit(Seq(
    "fe000097", //    00      	auipc	ra,0xfe000
    "00008093", //    04      	mv	ra,ra
    "0031c1b3", //    08      	xor	gp,gp,gp
    "00118193", //    0C      	addi	gp,gp,1
    "005b92b7", //    10      	lui	t0,0x5b9
    "d8028293", //    14      	addi	t0,t0,-640 # 5b8d80 <L2+0x5b8d58>
    "00424233", //    18      	xor	tp,tp,tp
    // L1
    "00519663", //    1C      	bne	gp,t0,<L2>
    "00024233", //    20      	xor	tp,tp,zero
    "0000a023", //    24      	sw	zero,0(ra) # fe000000 <DONE+0xff000000>
    // L2
    "00118193", //    28      	addi	gp,gp,1
    "ff1ff06f"  //    2C      	j	<L1>
  ).map(d => ("h" + d).U(32.W)))

  io.data := rom(io.address(31, 2))
}
