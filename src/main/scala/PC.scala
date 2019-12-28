import chisel3._

class PCPort(implicit val conf: CAHPConfig) extends Bundle {
  val jumpAddress = Input(UInt(conf.romAddrWidth.W))
  val pcDiff = Input(UInt(3.W))
  val jump = Input(Bool())
  val enable = Input(Bool())
  val pcOut = Output(UInt(conf.romAddrWidth.W))
}

class PC(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new PCPort)

  //**** Register Declaration ****
  val regPC = RegInit(0.U(conf.romAddrWidth.W))


  //**** I/O Connection ****
  io.pcOut := regPC


  //**** Sequential Circuit ****
  regPC := regPC
  when(io.enable) {
    when(io.jump === false.B) {
      regPC := regPC + io.pcDiff
    }.otherwise {
      regPC := io.jumpAddress
    }
  }
}
