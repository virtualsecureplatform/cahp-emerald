import chisel3._
import chisel3.util.Cat

class InstructionGetterIn(implicit val conf:CAHPConfig) extends Bundle {
  val instAddr = UInt(conf.romAddrWidth.W)
  val lowerBlock = UInt(64.W)
  val upperBlock = UInt(64.W)
}

class InstructionGetterOut(implicit val conf:CAHPConfig) extends Bundle {
  val inst = UInt(24.W)
  val isLong = Bool()
  val isLoadFromLowerLast = Bool()
  val isLoadFromUpper = Bool()
}

class InstructionGetterPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new InstructionGetterIn)
  val out = Output(new InstructionGetterOut)
}

class InstructionGetter(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new InstructionGetterPort)

  val inst = Wire(UInt(24.W))

  val instAddr = io.in.instAddr
  val lowerBlock = io.in.lowerBlock
  val upperBlock = io.in.upperBlock

  io.out.isLoadFromLowerLast := false.B
  io.out.isLoadFromUpper := false.B

  when(instAddr(2,0) === 0.U){
    inst := Cat(lowerBlock(23, 16), lowerBlock(15, 8), lowerBlock(7, 0))
  }.elsewhen(instAddr(2,0) === 1.U){
    inst := Cat(lowerBlock(31, 24), lowerBlock(23, 16), lowerBlock(15, 8))
  }.elsewhen(instAddr(2,0) === 2.U){
    inst := Cat(lowerBlock(39, 32), lowerBlock(31, 24), lowerBlock(23, 16))
  }.elsewhen(instAddr(2,0) === 3.U){
    inst := Cat(lowerBlock(47, 40), lowerBlock(39, 32), lowerBlock(31, 24))
  }.elsewhen(instAddr(2,0) === 4.U){
    inst := Cat(lowerBlock(55, 48), lowerBlock(47, 40), lowerBlock(39, 32))
  }.elsewhen(instAddr(2,0) === 5.U){
    inst := Cat(lowerBlock(63, 56), lowerBlock(55, 48), lowerBlock(47, 40))
    when(inst(0)){
      io.out.isLoadFromLowerLast := true.B
    }
  }.elsewhen(instAddr(2,0) === 6.U){
    inst := Cat(upperBlock(7, 0), lowerBlock(63, 56), lowerBlock(55, 48))
    when(!inst(0)){
      io.out.isLoadFromLowerLast := true.B
    }.otherwise{
      io.out.isLoadFromUpper := true.B
    }
  }.otherwise {
    inst := Cat(upperBlock(15, 8), upperBlock(7, 0), lowerBlock(63, 56))
    io.out.isLoadFromUpper := true.B
  }

  io.out.isLong := inst(0)
}