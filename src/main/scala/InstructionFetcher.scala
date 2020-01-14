import chisel3._
import chisel3.util.Cat

class InstructionFetcherIn(implicit val conf:CAHPConfig) extends Bundle {
  val romData = UInt(64.W)
  val pcAddress = UInt(conf.romAddrWidth.W)

  val jump = Bool()
  val jumpAddress = UInt(conf.romAddrWidth.W)

}

class InstructionFetcherOut(implicit val conf:CAHPConfig) extends Bundle {
  val instA = UInt(24.W)
  val instB = UInt(24.W)

  val romAddr = UInt((conf.romAddrWidth-3).W)
  val pcAddr = UInt(conf.romAddrWidth.W)

  val pcDiff = UInt(3.W)
}

class InstructionFetcherPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new InstructionFetcherIn)
  val out = Output(new InstructionFetcherOut)

  val enable = Input(Bool())
  val state = Output(Bool())
}

object romCacheStateType {
  val NotLoaded = false.B
  val Loaded = true.B
}

class InstructionFetcher(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new InstructionFetcherPort)

  val depSolver = Module(new DependencySolver())
  val instAGetter = Module(new InstructionGetter())
  val instBGetter = Module(new InstructionGetter())

  val romCache = Reg(UInt(64.W))
  val romCacheState = RegInit(romCacheStateType.NotLoaded)

  val romAddr = Wire(UInt((conf.romAddrWidth - 3).W))

  val instAAddr = Wire(UInt(conf.romAddrWidth.W))
  val instBAddr = Wire(UInt(conf.romAddrWidth.W))

  val instAValid = Wire(Bool())
  val instBValid = Wire(Bool())

  val pcDiff = Wire(UInt(3.W))

  io.state := romCacheState
  instAGetter.io.in.lowerBlock := io.in.romData
  instAGetter.io.in.upperBlock := io.in.romData
  instAGetter.io.in.instAddr := instAAddr

  instBGetter.io.in.lowerBlock := io.in.romData
  instBGetter.io.in.upperBlock := io.in.romData
  instBGetter.io.in.instAddr := instBAddr

  depSolver.io.instA := instAGetter.io.out.inst
  depSolver.io.instB := instBGetter.io.out.inst
  depSolver.io.instAValid := instAValid
  depSolver.io.instBValid := instBValid

  when(io.in.jump) {
    instAAddr := io.in.jumpAddress
  }.otherwise {
    instAAddr := io.in.pcAddress
  }

  when(instAGetter.io.out.isLong) {
    instBAddr := instAAddr + 3.U
  }.otherwise {
    instBAddr := instAAddr + 2.U
  }

  when(depSolver.io.instAExec) {
    when(depSolver.io.instBExec) {
      when(instAGetter.io.out.isLong) {
        when(instBGetter.io.out.isLong) {
          pcDiff := 6.U
        }.otherwise {
          pcDiff := 5.U
        }
      }.otherwise {
        when(instBGetter.io.out.isLong) {
          pcDiff := 5.U
        }.otherwise {
          pcDiff := 4.U
        }
      }
      io.out.pcAddr := instBAddr
    }.otherwise {
      when(instAGetter.io.out.isLong) {
        pcDiff := 3.U
      }.otherwise {
        pcDiff := 2.U
      }
      io.out.pcAddr := instAAddr
    }
  }.otherwise {
    pcDiff := 0.U
    io.out.pcAddr := instAAddr
  }
  io.out.pcDiff := pcDiff

  when(!io.enable){
    instAValid := false.B
    instBValid := false.B
    romAddr := instAAddr(conf.romAddrWidth-1, 3)
  }.elsewhen(io.in.jump || (romCacheState === romCacheStateType.NotLoaded)){
    romAddr := instAAddr(conf.romAddrWidth-1, 3)
    when(instAGetter.io.out.isLoadFromUpper) {
      instAValid := false.B
      instBValid := false.B
    }.elsewhen(instAGetter.io.out.isLoadFromLowerLast){
      instAValid := true.B
      instBValid := false.B
    }.elsewhen(instBGetter.io.out.isLoadFromUpper){
      instAValid := true.B
      instBValid := false.B
    }.otherwise{
      instAValid := true.B
      instBValid := true.B
    }
  }.otherwise{
    romAddr := instAAddr(conf.romAddrWidth-1, 3) + 1.U
    instAValid := true.B
    instBValid := true.B
    instAGetter.io.in.lowerBlock := romCache
    //Instruction B is Load From UpperBlock romData
    when(instAGetter.io.out.isLoadFromLowerLast || instAGetter.io.out.isLoadFromUpper){
      instBGetter.io.in.lowerBlock := io.in.romData
    }.otherwise{
      instBGetter.io.in.lowerBlock := romCache
    }
  }
  io.out.romAddr := romAddr

    when(io.in.jump || (romCacheState === romCacheStateType.NotLoaded)) {
      romCache := io.in.romData
      when(instBGetter.io.out.isLoadFromLowerLast & depSolver.io.instAExec & depSolver.io.instBExec) {
        romCacheState := romCacheStateType.NotLoaded
      }.elsewhen(instAGetter.io.out.isLoadFromLowerLast & depSolver.io.instAExec) {
        romCacheState := romCacheStateType.NotLoaded
      }.otherwise {
        romCacheState := romCacheStateType.Loaded
      }
    }.otherwise {
      when((instAAddr(2, 0) +& pcDiff(2, 0)) (3) === 1.U) {
        romCache := io.in.romData
      }.otherwise {
        romCache := romCache
      }

      romCacheState := romCacheStateType.Loaded
    }

  when(depSolver.io.instAExec){
    io.out.instA := instAGetter.io.out.inst
  }.otherwise{
    io.out.instA := 0.U
  }
  when(depSolver.io.instBExec){
    io.out.instB := instBGetter.io.out.inst
  }.otherwise{
    io.out.instB := 0.U
  }

}