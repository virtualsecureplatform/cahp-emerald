import chisel3.Module
import chisel3.{util, _}

class TestUnit(implicit val conf: CAHPConfig) extends Module{
  val io = IO(new Bundle{
    val ifOut = new IfUnitOut
    val exOut = Flipped(new ExUnitIn)
    val memOut = Flipped(new MemUnitIn)
    val wbOut = Flipped(new WbUnitIn)
    val stole = Output(Bool())
  })

  val ifUnit = Module(new IfUnit())
  val idWbUnit = Module(new IdWbUnit())
  val rom = Module(new ExternalTestRom)

  ifUnit.io.in.romData := rom.io.romData

  ifUnit.io.in.jumpAddress := 0.U
  ifUnit.io.in.jump := false.B
  ifUnit.io.enable := true.B

  rom.io.romAddress := ifUnit.io.out.romAddress

  idWbUnit.io.idIn.instA := ifUnit.io.out.instAOut
  idWbUnit.io.idIn.instB := ifUnit.io.out.instBOut
  idWbUnit.io.idIn.pc := DontCare

  idWbUnit.io.wbIn := DontCare
  idWbUnit.io.wbIn.instARegWrite.regWriteEnable := false.B
  idWbUnit.io.wbIn.instBRegWrite.regWriteEnable := false.B
  idWbUnit.io.exMemIn := DontCare
  idWbUnit.io.exMemIn.instAMemRead := false.B
  idWbUnit.io.exMemIn.instBMemRead := false.B
  idWbUnit.io.exWbIn := DontCare
  idWbUnit.io.exWbIn.instARegWrite.regWriteEnable := false.B
  idWbUnit.io.exWbIn.instBRegWrite.regWriteEnable := false.B
  idWbUnit.io.memWbIn := DontCare
  idWbUnit.io.memWbIn.instARegWrite.regWriteEnable := false.B
  idWbUnit.io.memWbIn.instBRegWrite.regWriteEnable := false.B
  idWbUnit.io.idEnable := true.B
  idWbUnit.io.wbEnable := true.B
  idWbUnit.io.flush := false.B

  io.ifOut := ifUnit.io.out
  io.exOut := idWbUnit.io.exOut
  io.memOut := idWbUnit.io.memOut
  io.wbOut := idWbUnit.io.wbOut
  io.stole := idWbUnit.io.stole
}
