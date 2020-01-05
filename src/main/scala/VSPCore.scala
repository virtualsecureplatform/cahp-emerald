import chisel3._

class MainRegisterOutPort(implicit val conf:CAHPConfig) extends Bundle{
  val x0 = Output(UInt(16.W))
  val x1 = Output(UInt(16.W))
  val x2 = Output(UInt(16.W))
  val x3 = Output(UInt(16.W))
  val x4 = Output(UInt(16.W))
  val x5 = Output(UInt(16.W))
  val x6 = Output(UInt(16.W))
  val x7 = Output(UInt(16.W))
  val x8 = Output(UInt(16.W))
  val x9 = Output(UInt(16.W))
  val x10 = Output(UInt(16.W))
  val x11 = Output(UInt(16.W))
  val x12 = Output(UInt(16.W))
  val x13 = Output(UInt(16.W))
  val x14 = Output(UInt(16.W))
  val x15 = Output(UInt(16.W))
}

class VSPCorePort(implicit val conf:CAHPConfig) extends Bundle {
  val romAddr = Output(UInt(7.W))
  val romData = Input(UInt(32.W))

  val finishFlag = Output(Bool())
  val regOut = new MainRegisterOutPort()
}

class VSPCore() extends Module{
  implicit val conf = CAHPConfig()
  conf.test = true
  val io = IO(new VSPCorePort)
  val coreUnit = Module(new CoreUnit)
  val memA = Module(new ExternalRam(false, ""))
  val memB = Module(new ExternalRam(false, ""))

  //io.finishFlag := coreUnit.io.finishFlag
  io.regOut := coreUnit.io.regOut

  io.romAddr := coreUnit.io.romAddr
  coreUnit.io.romData := io.romData

  memA.io.in := coreUnit.io.memA.in
  coreUnit.io.memA.out := memA.io.out
  memB.io.in := coreUnit.io.memB.in
  coreUnit.io.memB.out := memB.io.out
}
