import chisel3._

class DependencySolverPort(implicit val conf:CAHPConfig) extends Bundle {
  val instA = Input(UInt(24.W))
  val instAValid = Input(Bool())
  val instB = Input(UInt(24.W))
  val instBValid = Input(Bool())

  val instAExec = Output(Bool())
  val instBExec = Output(Bool())

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


  val instAExec = Wire(Bool())
  val instBExec = Wire(Bool())
  instAExec := true.B
  instBExec := true.B

  val instAType = DecoderUtils.getInstructionCategory(io.instA)
  val instBType = DecoderUtils.getInstructionCategory(io.instB)
  when(instAType === InstructionCategory.InstJ){
    instBExec := false.B
  }.elsewhen(instAType === InstructionCategory.InstM){
    when(instBType === InstructionCategory.InstM){
      instBExec := false.B
    }.otherwise{
      when(instBLong){
        //LI
        when(io.instB(5,0) === "b110101".U) {
          when(instBRd === instARd){
            instBExec := false.B
          }
        }.elsewhen(instBType === InstructionCategory.InstM){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            instBExec := false.B
          }
        }.elsewhen(instBType === InstructionCategory.InstR){
          when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
            instBExec := false.B
          }
        }.elsewhen(instBType === InstructionCategory.InstI){
          when((instBRd === instARd) || (instBRs1 === instARd )){
            instBExec := false.B
          }
        }.otherwise{
          when((instBRd === instARd) || (instBRs1 === instARd )){
            instBExec := false.B
          }
        }
      }.otherwise {
        when(instBType === InstructionCategory.InstM) {
          when((instBRd === instARd)) {
            instBExec := false.B
          }.otherwise{
            when(instARd === 1.U){
              instBExec := false.B
            }
          }
        }.elsewhen(instBType === InstructionCategory.InstR) {
          when((instBRd === instARd) || (instBRs1 === instARd)) {
            instBExec := false.B
          }
        }.elsewhen(instBType === InstructionCategory.InstI) {
          when((instBRd === instARd)) {
            instBExec := false.B
          }
        }.otherwise {
          //JALR, JR
          when((instBRd === instARd) && io.instB(3) === 0.U) {
            instBExec := false.B
          }
        }
      }
    }
  }.otherwise{
    when(instBLong){
      //LI
      when(io.instB(5,0) === "b110101".U) {
        when(instBRd === instARd){
          instBExec := false.B
        }
      }.elsewhen(instBType === InstructionCategory.InstM){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          instBExec := false.B
        }
      }.elsewhen(instBType === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )|| (instBRs2 === instARd)){
          instBExec := false.B
        }
      }.elsewhen(instBType === InstructionCategory.InstI){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          instBExec := false.B
        }
      }.otherwise{
        when((instBRd === instARd) || (instBRs1 === instARd )){
          instBExec := false.B
        }
      }
    }.otherwise{
      when(instBType === InstructionCategory.InstM){
        when((instBRd === instARd)) {
          instBExec := false.B
        }.otherwise{
          when(instARd === 1.U){
            instBExec := false.B
          }
        }
    }.elsewhen(instBType === InstructionCategory.InstR){
        when((instBRd === instARd) || (instBRs1 === instARd )){
          instBExec := false.B
        }
      }.elsewhen(instBType === InstructionCategory.InstI){
        when((instBRd === instARd)){
          instBExec := false.B
        }
      }.otherwise{
        //JALR, JR
        when((instBRd === instARd) && io.instB(3) === 0.U){
          instBExec := false.B
        }
      }
    }
  }

  io.execA := instAExec&io.instAValid
  io.execB := instBExec&io.instBValid

  io.instAExec := instAExec&io.instAValid
  io.instBExec := instBExec&io.instBValid
}
