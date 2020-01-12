import chisel3._
import chisel3.util.Cat

class InstructionFetcherIn(implicit val conf:CAHPConfig) extends Bundle {
  val romData = UInt(64.W)
  val pcAddress = UInt(conf.romAddrWidth.W)

  val jump = Bool()
  val jumpAddress = UInt(conf.romAddrWidth.W)

  val enable = Bool()
}

class InstructionFetcherOut(implicit val conf:CAHPConfig) extends Bundle {
  val instA = UInt(24.W)
  val instB = UInt(24.W)

  val romAddr = UInt((conf.romAddrWidth-3).W)

  val pcDiff = UInt(3.W)
}

class InstructionFetcherPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new InstructionFetcherIn)
  val out = Output(new InstructionFetcherOut)
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

  val romAddr = Wire(UInt((conf.romAddrWidth-3).W))

  val instAAddr = Wire(UInt(conf.romAddrWidth.W))
  val instBAddr = Wire(UInt(conf.romAddrWidth.W))

  val instAValid = Wire(Bool())
  val instBValid = Wire(Bool())

  val pcDiff = Wire(UInt(3.W))

  instAGetter.io.in.lowerBlock := romCache
  instAGetter.io.in.upperBlock := io.in.romData
  instAGetter.io.in.instAddr := instAAddr

  instBGetter.io.in.lowerBlock := romCache
  instBGetter.io.in.upperBlock := io.in.romData
  instBGetter.io.in.instAddr := instBAddr

  depSolver.io.instA := instAGetter.io.out.inst
  depSolver.io.instB := instBGetter.io.out.inst
  depSolver.io.instAValid := instAValid
  depSolver.io.instBValid := instBValid

  when(io.in.jump){
    instAAddr := io.in.jumpAddress
  }.otherwise{
    instAAddr := io.in.pcAddress
  }

  when(instAGetter.io.out.isLong){
    instBAddr := instAAddr + 3.U
  }.otherwise{
    instBAddr := instAAddr + 2.U
  }

  when(depSolver.instAExec){
    when(depSolver.instBExec) {
      when(instAGetter.io.out.isLong){
        when(instBGetter.io.out.isLong){
          pcDiff := 6.U
        }.otherwise{
          pcDiff := 5.U
        }
      }.otherwise{
        when(instBGetter.io.out.isLong){
          pcDiff := 5.U
        }.otherwise{
          pcDiff := 4.U
        }
      }
    }.otherwise{
      when(instAGetter.io.out.isLong){
        pcDiff := 3.U
      }.otherwise{
        pcDiff := 2.U
      }
    }
  }.otherwise{
    pcDiff := 0.U
  }
  io.out.pcDiff := pcDiff

  when(io.in.jump || (romCacheState === romCacheStateType.NotLoaded)){
    romAddr := instAAddr(conf.romAddrWidth-2, 3)
    when(instAGetter.io.out.isLoadFromUpper){
      instAValid := false.B
      instBValid := false.B
    }.elsewhen(instBGetter.io.out.isLoadFromUpper){
      instAValid := true.B
      instBValid := false.B
    }.otherwise{
      instAValid := true.B
      instBValid := true.B
    }
  }.otherwise{
    romAddr := instAAddr(conf.romAddrWidth-2, 3) + 1.U
    instAValid := true.B
    instBValid := true.B
    //Instruction B is Load From UpperBlock romData
    when(instAGetter.io.out.isLoadFromLowerLast || instAGetter.io.out.isLoadFromUpper){
      instBGetter.io.in.lowerBlock := io.in.romData
    }
  }

  when(io.in.jump || (romCacheState === romCacheStateType.NotLoaded)){
    romCache := io.in.romData
    when(instBGetter.io.out.isLoadFromLowerLast & depSolver.io.instAExec & depSolver.io.instBExec) {
      romCacheState := romCacheStateType.NotLoaded
    }.elsewhen(instAGetter.io.out.isLoadFromLowerLast&depSolver.io.instAExec){
      romCacheState := romCacheStateType.NotLoaded
    }.otherwise{
      romCacheState := romCacheStateType.Loaded
    }
  }.otherwise{
    when((instAAddr(2,0)+&pcDiff(2,0))(3) === 1.U){
      romCache := io.in.romData
    }.otherwise{
      romCache := romCache
    }

    romCacheState := romCacheStateType.Loaded
  }

  when(depSolver.instAExec){
    io.out.instA := instAGetter.io.out.inst
  }.otherwise{
    io.out.instA := 0.U
  }
  when(depSolver.instBExec){
    io.out.instB := instBGetter.io.out.inst
  }.otherwise{
    io.out.instB := 0.U
  }

}