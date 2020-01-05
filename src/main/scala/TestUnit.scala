import chisel3.Module
import chisel3.{util, _}

class TestUnit(val memAInit:Seq[BigInt], val memBInit:Seq[BigInt])(implicit val conf: CAHPConfig) extends Module{
  val io = IO(new Bundle{
    val load = Input(Bool())
    val ifOut = new IfUnitOut
    val exOut = Output(new ExUnitIn)
    val memOut = Output(new MemUnitIn)
    val wbOut = Output(new WbUnitIn)
    val stole = Output(Bool())

    val exUnitOut = new ExUnitOut
    val memUnitOut = new MemUnitOut
    val memWbOut = Output(new WbUnitIn)
    val memAIn = Output(new MemPortIn)
    val memBIn = Output(new MemPortIn)
  })

  val ifUnit = Module(new IfUnit())
  val idWbUnit = Module(new IdWbUnit())
  val exUnit = Module(new ExUnit())
  val memUnit = Module(new MemUnit())
  val rom = Module(new ExternalTestRom)
  val memA = Module(new ExternalTestRam(memAInit))
  val memB = Module(new ExternalTestRam(memBInit))

  ifUnit.io.in.romData := rom.io.romData

  ifUnit.io.in.jumpAddress := exUnit.io.out.jumpAddress
  ifUnit.io.in.jump := exUnit.io.out.jump
  ifUnit.io.idStole := idWbUnit.io.stole

  rom.io.romAddress := ifUnit.io.out.romAddress

  idWbUnit.io.idIn.instA := ifUnit.io.out.instAOut
  idWbUnit.io.idIn.instB := ifUnit.io.out.instBOut
  idWbUnit.io.idIn.pc := ifUnit.io.out.pcAddress

  idWbUnit.io.exMemIn := exUnit.io.memOut
  idWbUnit.io.exWbIn := exUnit.io.wbOut
  idWbUnit.io.memWbIn := memUnit.io.wbOut

  idWbUnit.io.flush := false.B

  exUnit.io.in := idWbUnit.io.exOut
  exUnit.io.memIn := idWbUnit.io.memOut
  exUnit.io.wbIn := idWbUnit.io.wbOut
  exUnit.io.flush := exUnit.io.out.jump

  memUnit.io.in := exUnit.io.memOut
  memUnit.io.wbIn := exUnit.io.wbOut

  memA.io.in := memUnit.io.memA.in
  memUnit.io.memA.out := memA.io.out

  memB.io.in := memUnit.io.memB.in
  memUnit.io.memB.out := memB.io.out

  idWbUnit.io.wbIn := memUnit.io.wbOut

  io.ifOut := ifUnit.io.out
  io.exOut := idWbUnit.io.exOut
  io.memOut := idWbUnit.io.memOut
  io.wbOut := idWbUnit.io.wbOut
  io.stole := idWbUnit.io.stole

  io.exUnitOut := exUnit.io.out
  io.memUnitOut := memUnit.io.out
  io.memWbOut := memUnit.io.wbOut
  io.memAIn := memA.io.in
  io.memBIn := memB.io.in

  if(conf.load){
    ifUnit.io.enable := !io.load
    idWbUnit.io.idEnable := !io.load
    exUnit.io.enable := !io.load
    memUnit.io.enable := !io.load
    idWbUnit.io.wbEnable := !io.load
    memA.io.in.load := io.load
    memB.io.in.load := io.load
  }else{
    ifUnit.io.enable := true.B
    idWbUnit.io.idEnable := true.B
    exUnit.io.enable := true.B
    memUnit.io.enable := true.B
    idWbUnit.io.wbEnable := true.B
    memA.io.in.load := false.B
    memB.io.in.load := false.B
  }
}
