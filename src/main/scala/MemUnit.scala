/*
Copyright 2019 Naoki Matsumoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import chisel3._
import chisel3.util.Cat

class MemPortIn(implicit val conf:CAHPConfig) extends Bundle {
  val in = UInt(8.W)
  val address = UInt(16.W)
  val writeEnable = Bool()
  val load = Bool()

  override def cloneType: this.type = new MemPortIn().asInstanceOf[this.type]
}

class MemPort(implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new MemPortIn)
  val out = Output(UInt(8.W))
}

class MemUnitIn (implicit val conf:CAHPConfig) extends Bundle {
  val in =  UInt(16.W)
  val address = UInt(16.W)
  val instAMemRead = Bool()
  val instBMemRead = Bool()
  val memWrite = Bool()
  val byteEnable = Bool()
  val signExt = Bool()
}

class MemUnitOut (implicit val conf:CAHPConfig) extends Bundle {
  val out = Output(UInt(16.W))
  val fwdData = Output(UInt(16.W))

}

class MemUnitPort (implicit val conf:CAHPConfig) extends Bundle {
  val in = Input(new MemUnitIn)
  val out = new MemUnitOut
  val wbIn = Input(new WbUnitIn)

  val memA = Flipped(new MemPort())
  val memB = Flipped(new MemPort())
  val wbOut = Output(new WbUnitIn)

  val enable = Input(Bool())
}

class MemUnitTestPort extends Bundle{
  val in =  Input(UInt(16.W))
  val address = Input(UInt(16.W))

  val instAMemRead = Input(Bool())
  val instBMemRead = Input(Bool())
  val memWrite = Input(Bool())
  val byteEnable = Input(Bool())
  val signExt = Input(Bool())
  val Enable = Input(Bool())

  val out = Output(UInt(16.W))
}
class MemUnitTest(val memAInit:Seq[BigInt], val memBInit:Seq[BigInt])(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new MemUnitTestPort)
  val unit = Module(new MemUnit)
  val memA = Module(new ExternalRam(false, ""))
  val memB = Module(new ExternalRam(false, ""))


  unit.io.in.in := io.in
  unit.io.in.address := io.address
  unit.io.in.instAMemRead := io.instAMemRead
  unit.io.in.instBMemRead := io.instBMemRead
  unit.io.in.memWrite := io.memWrite
  unit.io.in.byteEnable := io.byteEnable
  unit.io.in.signExt := io.signExt
  unit.io.enable := io.Enable
  memA.io.in := unit.io.memA.in
  unit.io.memA.out := memA.io.out
  memB.io.in := unit.io.memB.in.in
  unit.io.memB.out := memB.io.out
  io.out := unit.io.out.out
}

class MemUnit(implicit val conf:CAHPConfig) extends Module {
  val io = IO(new MemUnitPort)

  val pMemReg = RegInit(0.U.asTypeOf(new MemUnitIn))
  val pWbReg = RegInit(0.U.asTypeOf(new WbUnitIn))

  when(io.enable){
    pMemReg := io.in
    pWbReg := io.wbIn
  }

  io.out.fwdData := io.out.out
  io.wbOut := pWbReg

  when(pMemReg.instAMemRead){
    io.wbOut.instARegWrite.regWriteData := io.out.out
  }.elsewhen(pMemReg.instBMemRead){
    io.wbOut.instBRegWrite.regWriteData := io.out.out
  }

  def sign_ext_8bit(v:UInt) : UInt = {
    val res = Wire(UInt(16.W))
    when(v(7,7) === 1.U){
      res := Cat(0xFF.U(8.W), v)
    }.otherwise{
      res := v
    }
    res
  }

  val addr = pMemReg.address(8,1)
  val data_upper = pMemReg.in(15, 8)
  val data_lower = pMemReg.in(7, 0)

  io.memA.in.address := addr
  io.memB.in.address := addr
  io.memA.in.in := data_upper
  io.memB.in.in := data_lower
  io.memA.in.writeEnable := false.B
  io.memB.in.writeEnable := false.B
  io.memA.in.load := DontCare
  io.memB.in.load := DontCare

  when(pMemReg.byteEnable){
    when(pMemReg.memWrite){
      when(pMemReg.address(0) === 1.U) {
        io.memA.in.writeEnable := true.B&io.enable
        io.memA.in.in := data_lower
      }.otherwise {
        io.memB.in.writeEnable := true.B&io.enable
        io.memB.in.in := data_lower
      }
    }
  }.otherwise {
    when(pMemReg.memWrite) {
      io.memA.in.writeEnable := true.B&io.enable
      io.memB.in.writeEnable := true.B&io.enable
    }
  }

  when(pMemReg.byteEnable){
    when(pMemReg.address(0) === 1.U){
      when(pMemReg.signExt){
        io.out.out := sign_ext_8bit(io.memA.out)
      }.otherwise{
        io.out.out := Cat(0.U(8.W), io.memA.out)
      }
    }.otherwise{
      when(pMemReg.signExt){
        io.out.out := sign_ext_8bit(io.memB.out)
      }.otherwise{
        io.out.out := Cat(0.U(8.W), io.memB.out)
      }
    }
  }.otherwise{
    io.out.out := Cat(io.memA.out, io.memB.out)
  }

  when(conf.debugMem.B){
    when(pMemReg.instAMemRead) {
      printf("[instA MEM] MemRead Mem[0x%x] => Data:0x%x\n", pMemReg.address, io.out.out)
    }
    when(pMemReg.instBMemRead) {
      printf("[instB MEM] MemRead Mem[0x%x] => Data:0x%x\n", pMemReg.address, io.out.out)
    }
    when(io.in.memWrite) {
      printf("[MEM] MemWrite Mem[0x%x] <= Data:0x%x\n", io.in.address, io.in.in)
    }
  }
}
