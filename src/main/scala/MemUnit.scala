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

class MemPort(val conf:CAHPConfig) extends Bundle {
  val in = Input(UInt(8.W))
  val address = Input(UInt(16.W))
  val writeEnable = Input(Bool())
  val out = Output(UInt(8.W))
  val load = Input(Bool())

  override def cloneType: this.type = new MemPort(conf).asInstanceOf[this.type]
}

class MemUnitIn (implicit val conf:CAHPConfig) extends Bundle {
  val in =  Input(UInt(16.W))
  val address = Input(UInt(16.W))
  val instAMemRead = Input(Bool())
  val instBMemRead = Input(Bool())
  val memWrite = Input(Bool())
  val byteEnable = Input(Bool())
  val signExt = Input(Bool())
}

class MemUnitOut (implicit val conf:CAHPConfig) extends Bundle {
  val out = Output(UInt(16.W))
  val fwdData = Output(UInt(16.W))

}

class MemUnitPort (implicit val conf:CAHPConfig) extends Bundle {
  val in = new MemUnitIn
  val out = new MemUnitOut
  val wbIn = new WbUnitIn

  val memA = Flipped(new MemPort(conf))
  val memB = Flipped(new MemPort(conf))
  val wbOut = Flipped(new WbUnitIn)

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
  val memA = Module(new ExternalTestRam(memAInit))
  val memB = Module(new ExternalTestRam(memBInit))


  unit.io.in.in := io.in
  unit.io.in.address := io.address
  unit.io.in.instAMemRead := io.instAMemRead
  unit.io.in.instBMemRead := io.instBMemRead
  unit.io.in.memWrite := io.memWrite
  unit.io.in.byteEnable := io.byteEnable
  unit.io.in.signExt := io.signExt
  unit.io.enable := io.Enable
  memA.io.address := unit.io.memA.address
  memA.io.in := unit.io.memA.in
  memA.io.writeEnable := unit.io.memA.writeEnable
  memA.io.load := false.B
  unit.io.memA.out := memA.io.out
  memB.io.address := unit.io.memB.address
  memB.io.in := unit.io.memB.in
  memB.io.writeEnable := unit.io.memB.writeEnable
  memB.io.load := false.B
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

  val addr = Wire(UInt(8.W))
  val data_upper = io.in.in(15, 8)
  val data_lower = io.in.in(7, 0)
  addr := io.in.address(8,1)

  io.memA.address := addr
  io.memB.address := addr
  io.memA.in := data_upper
  io.memB.in := data_lower
  io.memA.writeEnable := false.B
  io.memB.writeEnable := false.B
  io.memA.load := DontCare
  io.memB.load := DontCare

  when(io.in.byteEnable){
    when(io.in.memWrite){
      when(io.in.address(0) === 1.U) {
        io.memA.writeEnable := true.B&io.enable
        io.memA.in := data_lower
      }.otherwise {
        io.memB.writeEnable := true.B&io.enable
        io.memB.in := data_lower
      }
    }
  }.otherwise {
    when(io.in.memWrite) {
      io.memA.writeEnable := true.B&io.enable
      io.memB.writeEnable := true.B&io.enable
    }
  }

  io.out.out := DontCare
  when(pMemReg.instAMemRead|pMemReg.instBMemRead){
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
  }.otherwise{
    io.out.out := pMemReg.address
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
