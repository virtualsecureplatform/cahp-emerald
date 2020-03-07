import chisel3.{Bundle, Flipped, Input, Module, Output, _}

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
  val romAddr = Output(UInt((conf.romAddrWidth-3).W))
  val romData = Input(UInt(64.W))

  val finishFlag = Output(Bool())
  val regOut = new MainRegisterOutPort()
}

class VSPCoreNoROM extends Module{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  conf.test = true
  val io = IO(new VSPCorePort)
  val coreUnit = Module(new CoreUnit)
  val memA = Module(new ExternalRam(false, ""))
  val memB = Module(new ExternalRam(false, ""))

  io.finishFlag := coreUnit.io.finish
  io.regOut := coreUnit.io.regOut

  io.romAddr := coreUnit.io.romAddr
  coreUnit.io.romData := io.romData
  coreUnit.io.load := false.B

  memA.io.in := coreUnit.io.memA.in
  coreUnit.io.memA.out := memA.io.out
  memB.io.in := coreUnit.io.memB.in
  coreUnit.io.memB.out := memB.io.out
}

class VSPCoreTest(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new Bundle{
    val finishFlag = Output(Bool())
    val regOut = new MainRegisterOutPort()
  })

  val vspCore = Module(new VSPCoreNoROM)
  val rom = Module(new ExternalTestRom)

  vspCore.io.romData := rom.io.romData
  rom.io.romAddress := vspCore.io.romAddr
  io.finishFlag := vspCore.io.finishFlag
  io.regOut := vspCore.io.regOut
}

class VSPCoreNoRAMROM extends Module {
  implicit val conf = CAHPConfig()
  conf.test = true
  conf.load = false
  val io = IO(new Bundle{
    val memA = Flipped(new MemPort())
    val memB = Flipped(new MemPort())
    val romAddr = Output(UInt((conf.romAddrWidth-3).W))
    val romData = Input(UInt(64.W))

    val finishFlag = Output(Bool())
    val regOut = new MainRegisterOutPort()
  })

  val coreUnit = Module(new CoreUnit)

  io.finishFlag := coreUnit.io.finish
  io.regOut := coreUnit.io.regOut
  io.romAddr := coreUnit.io.romAddr
  coreUnit.io.romData := io.romData
  coreUnit.io.load := false.B

  coreUnit.io.memA.out := io.memA.out
  coreUnit.io.memB.out := io.memB.out
  io.memA.in := coreUnit.io.memA.in
  io.memB.in := coreUnit.io.memB.in
}
