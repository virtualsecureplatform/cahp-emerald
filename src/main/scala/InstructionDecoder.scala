import chisel3.{util, _}

class InstructionDecoderPort(implicit val conf:CAHPConfig) extends Bundle {
  val inst = Input(UInt(24.W))
  val pc = Input(UInt(9.W))

  val imm = Output(UInt(16.W))
  val pcImm = Output(UInt(16.W))
  val pcImmSel = Output(Bool())
  val rs1 = Output(UInt(4.W))
  val rs2 = Output(UInt(4.W))
  val rd = Output(UInt(4.W))
  val longInst = Output(Bool())
  val inASel = Output(Bool())
  val inBSel = Output(Bool())
  val isJump = Output(Bool())
  val isMem = Output(Bool())
  val isFinish = Output(Bool())

  val exALUOut = Flipped(new ALUPortIn)
  val exBCOut = Flipped(new BranchControllerIn())
  val memOut = Flipped(new MemUnitIn)
  val wbOut = Flipped(new MainRegisterWritePortIn())

  val testImmType = if(conf.test) Output(UInt(4.W)) else Output(UInt(0.W))
  val testPCImmType = if(conf.test) Output(UInt(2.W)) else Output(UInt(0.W))
}

class InstructionDecoder(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new InstructionDecoderPort)

  io.isJump := false.B
  io.isMem := false.B

  io.imm := DecoderUtils.genImm(io.inst, DecoderUtils.getImmType(io.inst))
  io.pcImm := DecoderUtils.genPCImm(io.inst, DecoderUtils.getPCImmType(io.inst))
  io.pcImmSel := DecoderUtils.getPCImmSel(io.inst)
  io.testImmType := DecoderUtils.getImmType(io.inst)
  io.testPCImmType := DecoderUtils.getPCImmType(io.inst)

  io.inASel := DecoderUtils.getInASel(io.inst)
  io.inBSel := DecoderUtils.getInBSel(io.inst)
  io.exALUOut.opcode := DecoderUtils.getExOpcode(io.inst)
  io.exALUOut.inA := DontCare
  io.exALUOut.inB := DontCare
  io.exBCOut.pcOpcode := DecoderUtils.getPCOpcode(io.inst)
  io.exBCOut.pcImm := DontCare
  io.exBCOut.pc := DontCare
  io.exBCOut.pcAdd := DontCare
  io.memOut.instAMemRead := DecoderUtils.getMemRead(io.inst)
  io.memOut.instBMemRead := DontCare
  io.memOut.memWrite := DecoderUtils.getMemWrite(io.inst)
  io.memOut.byteEnable := DecoderUtils.getMemByte(io.inst)
  io.memOut.signExt := DecoderUtils.getMemSignExt(io.inst)
  io.memOut.address := DontCare
  io.memOut.in := DontCare

  io.isMem := io.memOut.instAMemRead || io.memOut.memWrite

  io.longInst := (io.inst(0) === 1.U)

  when(io.longInst) {
    io.rs1 := io.inst(15, 12)
    when(io.inst(2, 1) === InstructionCategory.InstM || io.inst(2, 1) === InstructionCategory.InstJ){
      io.rs2 := io.inst(11, 8)
    }.otherwise{
      io.rs2 := io.inst(19,16)
    }
  }.otherwise{
    when(io.inst(2, 1) === InstructionCategory.InstM){
      io.rs1 := 1.U(4.W)
      io.rs2 := io.inst(11, 8)
    }.otherwise{
      io.rs1 := io.inst(11, 8)
      io.rs2 := io.inst(15, 12)
    }
  }
  when(io.inst(2,1) === InstructionCategory.InstJ){
    io.rd := 0.U(4.W)
    io.isJump := true.B
  }.otherwise{
    io.rd := io.inst(11, 8)
  }

  io.wbOut.regWrite := io.rd
  io.wbOut.regWriteEnable := DecoderUtils.getRegWrite(io.inst)
  io.wbOut.regWriteData := DontCare
  io.isFinish := (io.inst(15,0) === 0xE.U)
}
