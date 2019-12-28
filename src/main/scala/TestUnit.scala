import chisel3.Module
import chisel3.{util, _}

class TestUnit(implicit val conf: CAHPConfig) extends Module{
  val io = IO(new IfUnitOut)

  val ifUnit = Module(new IfUnit())
  val rom = Module(new ExternalTestRom)

  ifUnit.io.in.romData := rom.io.romData

  ifUnit.io.in.jumpAddress := 0.U
  ifUnit.io.in.jump := false.B
  ifUnit.io.enable := true.B

  rom.io.romAddress := ifUnit.io.out.romAddress

  io := ifUnit.io.out
}
