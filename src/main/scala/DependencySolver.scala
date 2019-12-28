import chisel3._

class DependencySolverPort(implicit val conf:CAHPConfig) extends Bundle {
  val instA = Input(UInt(24.W))
  val instB = Input(UInt(24.W))
  val execA = Output(Bool())
  val execB = Output(Bool())
}

class DependencySolver(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new DependencySolverPort())

  val instARegWrite = DecoderUtils.getRegWrite(io.instA)
  val instBRegWrite = DecoderUtils.getRegWrite(io.instB)
  val instARd = DecoderUtils.getRegRd(io.instA)
  val instBLong = io.instB(0) === 1.U

  val instBRd = io.instB(11, 8)
  val instBRs1 = io.instB(15, 12)
  val instBRs2 = io.instB(19, 16)


  io.execA := true.B
  io.execB := true.B
  when(io.instA(2,1) === InstructionCategory.InstJ){
    io.execB := false.B
  }.elsewhen(io.instA(2,1) === InstructionCategory.InstM){
    when(io.instB(2,1) === InstructionCategory.InstM){
      io.execB := false.B
    }.otherwise{
      when(instBLong){
        //LI
        when(io.instB(5,0) === "b110101".U) {
          when(instBRd === instARd){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstM){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
          when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
            io.execB := false.B
          }
        }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }.otherwise{
          when((instBRd === instARd) || (instBRs1 === instARd )){
            io.execB := false.B
          }
        }
      }.otherwise {
        when(io.instB(2, 1) === InstructionCategory.InstM) {
          when((instBRd === instARd)) {
            io.execB := false.B
          }
        }.elsewhen(io.instB(2, 1) === InstructionCategory.InstR) {
          when((instBRd === instARd) || (instBRs1 === instARd)) {
            io.execB := false.B
          }
        }.elsewhen(io.instB(2, 1) === InstructionCategory.InstI) {
          when((instBRd === instARd)) {
            io.execB := false.B
          }
        }.otherwise {
          //JALR, JR
          when((instBRd === instARd) && io.instB(3) === 0.U) {
            io.execB := false.B
          }
        }
      }
    }
  }.otherwise{
    when(instBLong){
      //LI
      when(io.instB(5,0) === "b110101".U) {
        when(instBRd === instARd){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstM){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.otherwise{
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }
    }.otherwise{
      when(io.instB(2,1) === InstructionCategory.InstM){
        when((instBRd === instARd)){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          io.execB := false.B
        }
      }.elsewhen(io.instB(2,1) === InstructionCategory.InstI){
        when((instBRd === instARd)){
          io.execB := false.B
        }
      }.otherwise{
        //JALR, JR
        when((instBRd === instARd) && io.instB(3) === 0.U){
          io.execB := false.B
        }
      }
    }
  }
}
