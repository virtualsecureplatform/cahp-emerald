import chisel3._
import chisel3.util.Cat

class InstRomPort extends Bundle {
  val address = Input(UInt(2.W))
  val out = Output(UInt(32.W))

}

class InstRom extends Module {
  val io = IO(new InstRomPort);

  def romData() = {
    val rawInst:Array[BigInt] = Array(BigInt(0x0E2A0835), BigInt(0), BigInt(0), BigInt(0))
    val times = (0 until 4).map(i => rawInst(i % (rawInst.size)).asUInt(32.W))
    VecInit(times)
  }

    io.out := romData()(io.address)
}
class YosysTestPort(implicit val conf:CAHPConfig) extends Bundle {
  val romAddr = Output(UInt(7.W))
  val romData = Input(UInt(32.W))

  val romDataOut = Output(UInt(32.W))
  val testRegx8 = Output(UInt(16.W))
  val testRegWriteData = Output(UInt(16.W))
  val testRegWrite = Output(UInt(3.W))
  val testRegWriteEnable = Output(Bool())
}
class YosysTest(val memAInit:Seq[BigInt], val memBInit:Seq[BigInt]) extends Module{
  implicit val conf = CAHPConfig()
  conf.test = true
  val io = IO(new YosysTestPort)
  val coreUnit = Module(new CoreUnit)
  val memA = Module(new ExternalTestRam(memAInit))
  val memB = Module(new ExternalTestRam(memBInit))
  io.testRegx8 := coreUnit.io.testRegx8
  io.testRegWrite := coreUnit.io.testRegWrite
  io.testRegWriteEnable := coreUnit.io.testRegWriteEnable
  io.testRegWriteData := coreUnit.io.testRegWriteData

  io.romAddr := coreUnit.io.romAddr
  coreUnit.io.romData := io.romData
  io.romDataOut := io.romData

  memA.io.address := coreUnit.io.memA.address
  memA.io.in := coreUnit.io.memA.in
  memA.io.writeEnable := coreUnit.io.memA.writeEnable
  coreUnit.io.memA.out := memA.io.out
  memB.io.address := coreUnit.io.memB.address
  memB.io.in := coreUnit.io.memB.in
  memB.io.writeEnable := coreUnit.io.memB.writeEnable
  coreUnit.io.memB.out := memB.io.out
}

class L1TestPort extends Bundle{
  val romAddr = Output(UInt(1.W))
  val romData = Input(UInt(32.W))

  val romDataOut = Output(UInt(32.W))
}
class L1Test extends Module{
  val io = IO(new L1TestPort)

  io.romAddr := 1.U
  io.romDataOut := io.romData
}

class YosysTest2Port(implicit val conf:CAHPConfig) extends Bundle {
  val romDataOut = Output(UInt(32.W))
  val romAddrOut = Output(UInt(7.W))
  val testRegx8 = Output(UInt(16.W))
}

class YosysTest2(val memAInit:Seq[BigInt], val memBInit:Seq[BigInt])(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new YosysTest2Port)
  val yosys = Module(new YosysTest(memAInit, memBInit))
  val rom = Module(new ExternalTestRom())

  rom.io.romAddress := yosys.io.romAddr
  yosys.io.romData := rom.io.romData

  io.romDataOut := rom.io.romData
  io.romAddrOut := rom.io.romAddress
  io.testRegx8 := yosys.io.testRegx8
}

class YosysTest3Port(implicit val conf:CAHPConfig) extends Bundle{
  val in1 = Input(UInt(4.W))
  val in2 = Input(UInt(4.W))
  val out = Output(UInt(4.W))
}
class YosysTest3(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new YosysTest3Port())
  io.out := io.in1 + io.in2
}
class TestRam(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new MemPort(conf))
  val mem = Mem(4, UInt(2.W))
  val pReg = RegInit(0.U.asTypeOf(new MemPort(conf)))

  pReg.address := io.address
  pReg.in := io.in
  pReg.writeEnable := io.writeEnable
  when(pReg.writeEnable){
    mem(pReg.address) := pReg.in
  }
  io.out := mem(pReg.address)
}

class Addr4bit() extends Module{
  val io = IO(new Bundle{
    val inA = Input(UInt(4.W))
    val inB = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })
  io.out := io.inA + io.inB
}

class And4bit() extends Module{
  val io = IO(new Bundle{
    val inA = Input(UInt(4.W))
    val inB = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })
  io.out := io.inA & io.inB
}

class And4_2bit() extends Module{
  val io = IO(new Bundle{
    val inA = Input(UInt(4.W))
    val inB = Input(UInt(4.W))
    val out = Output(UInt(2.W))
  })
  val tmp = Wire(UInt(4.W))
  tmp := io.inA & io.inB
  io.out := Cat(tmp(0)&tmp(1), tmp(2)&tmp(3))
}

class Pass4bit() extends Module {
  val io = IO(new Bundle{
    val in = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })

  io.out := io.in
}

class Register4bit() extends Module{
  val io = IO(new Bundle{
    val in = Input(UInt(4.W))
    val out = Output(UInt(4.W))
  })
  val reg = RegInit(0.U(4.W))

  reg := io.in
  io.out := reg
}

class Mux4bit() extends Module{
  val io = IO(new Bundle{
    val inA = Input(UInt(4.W))
    val inB = Input(UInt(4.W))
    val sel = Input(Bool())
    val out = Output(UInt(4.W))
  })

  when(!io.sel){
    io.out := io.inA
  }.otherwise{
    io.out := io.inB
  }
}
