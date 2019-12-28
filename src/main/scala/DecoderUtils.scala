import chisel3._
import chisel3.util.{Cat, Fill}

object InstructionCategory {
  def InstR:UInt = 0.U(2.W)
  def InstI:UInt = 1.U(2.W)
  def InstM:UInt = 2.U(2.W)
  def InstJ:UInt = 3.U(2.W)
}

object ImmType {
  def SImm10:UInt  = "b0001".U(4.W)
  def UImm7:UInt   = "b0010".U(4.W)
  def SImm6:UInt   = "b0100".U(4.W)
  def Imm2:UInt    = "b1000".U(4.W)
}

object PCImmType {
  def SImm10:UInt = "b01".U(2.W)
  def SImm11:UInt = "b10".U(2.W)
}

object PCOpcode {
  def SImm10:UInt = "b01".U(2.W)
  def SImm11:UInt = "b10".U(2.W)
}


object DecoderUtils {
  def getRegWrite(inst:UInt): Bool = {
    val regWrite = Wire(Bool())

    when(inst(2,1) === InstructionCategory.InstM){
      regWrite := inst(3) != 1.U
    }.elsewhen(inst(2,1) === InstructionCategory.InstJ) {
      regWrite := (inst(4) === 1.U) && (inst(0) === 0.U)
    }.elsewhen(inst(7,0) === 0.U){
      regWrite := false.B
    }.otherwise{
      regWrite := true.B
    }
    regWrite
  }

  def getRegRd(inst:UInt):UInt = {
    val rd = Wire(UInt(4.W))
    when(inst(2,1) === InstructionCategory.InstJ){
      rd := 0.U(4.W)
    }.otherwise{
      rd := inst(11, 8)
    }
    rd
  }

  def genImm(inst:UInt, immType:UInt):UInt = {
    val imm = Wire(UInt(16.W))
    imm:=DontCare
    when(immType === ImmType.SImm10){
      imm := Cat(Fill(7, inst(7)), inst(6), inst(23, 16))
    }.elsewhen(immType === ImmType.UImm7){
      imm := Cat(0.U(9.W), inst(7, 6), inst(15, 12), 0.U(1.W))
    }.elsewhen(immType === ImmType.SImm6){
      imm := Cat(Fill(11, inst(7)), inst(6), inst(15, 12))
    }.elsewhen(immType === ImmType.Imm2){
      imm := 2.U(16.W)
    }
    imm
  }

  def genPCImm(inst:UInt, pcImmType:UInt):UInt = {
    val imm = Wire(UInt(16.W))
    imm:=DontCare
    when(pcImmType === PCImmType.SImm10){
      imm := Cat(Fill(7, inst(7)), inst(6), inst(23, 16))
    }.elsewhen(pcImmType === PCImmType.SImm11){
      imm := Cat(Fill(6, inst(15)), inst(15, 5))
    }
    imm
  }

  def getImmType(inst:UInt):UInt = {
    val immType = Wire(UInt(5.W))
    immType := DontCare
    when(inst(0) === 1.U) {
      immType := ImmType.SImm10
    }.otherwise{
      when(inst(2, 1) === InstructionCategory.InstM){
        when(inst(5, 4) === 1.U){
          immType := ImmType.UImm7
        }.otherwise{
          immType := ImmType.SImm6
        }
      }.elsewhen(inst(2, 1) === InstructionCategory.InstI){
        immType := ImmType.SImm6
      }.elsewhen(inst(2, 1) === InstructionCategory.InstJ){
        immType := ImmType.Imm2
      }
    }
    immType
  }

  def getPCImmType(inst:UInt):UInt =  {
    val pcImmType = Wire(UInt(2.W))
    pcImmType := DontCare
    when(inst(0) === 1.U) {
      pcImmType := PCImmType.SImm10
    }.otherwise{
      pcImmType := PCImmType.SImm11
    }
    pcImmType
  }

  def getPCImmSel(inst:UInt):UInt = {
    val pcImmSel = Wire(Bool())
    pcImmSel := true.B
    when(inst(0) === 0.U){
      when(inst(3) === 0.U) {
        pcImmSel := false.B
      }
    }
    pcImmSel
  }

  def getPCOpcode(inst:UInt):UInt = {
    val pcOpcode = Wire(UInt(3.W))
    pcOpcode := 0.U(3.W)
    when(inst(0) === 1.U){
      when(inst(2, 1) === InstructionCategory.InstJ){
        pcOpcode := inst(5, 3)
      }
    }.otherwise{
      when(inst(2, 1) === InstructionCategory.InstJ){
        pcOpcode := 4.U(3.W)
      }
    }
    pcOpcode
  }

  def getExOpcode(inst:UInt): UInt = {
    val exOpcode = Wire(UInt(4.W))
    exOpcode := DontCare
    when(inst(2,1) === InstructionCategory.InstR) {
      exOpcode := Cat(inst(6, 3))
    }.elsewhen(inst(2, 1) === InstructionCategory.InstI){
      exOpcode := Cat(0.U(1.W), inst(5, 3))
    }.elsewhen(inst(2,1) === InstructionCategory.InstM){
      when((inst(5,4) === 3.U)||(inst(5, 0) === 4.U)){
        exOpcode := 8.U(4.W)
      }.otherwise{
        exOpcode := 0.U(4.W)
      }
    }.otherwise{
      when(inst(0) === 1.U){
        exOpcode := 1.U(4.W)
      }.otherwise{
        exOpcode := 0.U(4.W)
      }
    }
    exOpcode
  }

  def getMemWrite(inst:UInt): Bool = {
    val memWrite = Wire(Bool())
    memWrite := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      memWrite := inst(3) === 1.U
    }.otherwise{
      memWrite := false.B
    }
    memWrite
  }

  def getMemRead(inst:UInt): Bool = {
    val memRead = Wire(Bool())
    memRead := false.B
    when(inst(2, 1) === InstructionCategory.InstM){
      when(inst(3) === 0.U){
        when(inst(5, 0) != "b110101".U(6.W) && inst(5, 0) != "b110100".U(6.W) && inst(5, 0) != "b100".U(6.W)){
          memRead := true.B
        }
      }
    }
    memRead
  }

  def getMemByte(inst:UInt): Bool ={
    val byteEnable = Wire(Bool())
    byteEnable := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      byteEnable := false.B
      when(inst(0) === 1.U){
        //LB, LBU, SB
        when(inst(5, 3) === 4.U(3.W) ||
          inst(5, 3) === 0.U(3.W) ||
          inst(5, 3) === 1.U(3.W)){
          byteEnable := true.B
        }
      }
    }
    byteEnable
  }

  def getMemSignExt(inst:UInt): Bool = {
    val signExt = Wire(Bool())
    signExt := DontCare
    when(inst(2,1) === InstructionCategory.InstM){
      signExt := false.B
      //LB
      when(inst(0) === 1.U && inst(5) === 1.U){
        signExt := true.B
      }
    }
    signExt
  }

  def getInASel(inst:UInt): Bool = {
    val inASel = Wire(Bool())
    val isLongInst:Bool = (inst(0) === 1.U)
    inASel := false.B
    when(!isLongInst){
      when(inst(2, 1) === InstructionCategory.InstJ){
        inASel := true.B
      }
    }
    inASel
  }

  def getInBSel(inst:UInt): Bool = {
    val inBSel = Wire(Bool())
    val isLongInst:Bool = (inst(0) === 1.U)
    when(inst(2, 1) === InstructionCategory.InstR){
      inBSel := false.B
    }.otherwise{
      inBSel := true.B
      when(isLongInst) {
        when(inst(2, 1) === InstructionCategory.InstJ){
          inBSel := false.B
        }
      }
    }
    inBSel
  }
}

