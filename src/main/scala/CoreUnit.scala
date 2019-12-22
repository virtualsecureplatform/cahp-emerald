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

class CoreUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val romData = Input(UInt(32.W))
  val romAddr = Output(UInt(conf.romAddrWidth.W))
  val memA = Flipped(new MemPort(conf))
  val memB = Flipped(new MemPort(conf))

  val finishFlag = Output(Bool())
  val regOut = new MainRegisterOutPort()

  val testRegx8 = if (conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
  val testRegWriteData = if (conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
  val testRegWrite = if (conf.test) Output(UInt(3.W)) else Output(UInt(0.W))
  val testRegWriteEnable = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  val testFinish = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  val testClockIF = if (conf.test) Output(Bool()) else Output(UInt(0.W))
  val load = if(conf.load) Input(Bool()) else Input(UInt(0.W))
}

class CoreUnit(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new CoreUnitPort)


  val ifUnit = Module(new IfUnit)
  val idwbUnit = Module(new IdWbUnit)
  val exUnit = Module(new ExUnit)
  val memUnit = Module(new MemUnit)

  io.romAddr := ifUnit.io.out.romAddress

  ifUnit.io.in.jump := exUnit.io.out.jump
  ifUnit.io.in.jumpAddress := exUnit.io.out.jumpAddress
  ifUnit.io.in.romData := io.romData

  io.testRegx8 := idwbUnit.io.testRegx8
  io.testFinish := DontCare


  idwbUnit.io.idIn.inst := ifUnit.io.out.instOut
  idwbUnit.io.idIn.pc := ifUnit.io.out.pcAddress
  idwbUnit.io.exWbIn := exUnit.io.wbOut
  idwbUnit.io.exMemIn := exUnit.io.memOut
  idwbUnit.io.memWbIn := memUnit.io.wbOut
  idwbUnit.io.flush := exUnit.io.out.jump


  exUnit.io.in     := idwbUnit.io.exOut
  exUnit.io.memIn  := idwbUnit.io.memOut
  exUnit.io.wbIn   := idwbUnit.io.wbOut
  exUnit.io.flush  := exUnit.io.out.jump

  memUnit.io.in     := exUnit.io.memOut
  memUnit.io.wbIn   := exUnit.io.wbOut

  io.memA.address := memUnit.io.memA.address
  io.memA.in := memUnit.io.memA.in
  io.memA.writeEnable := memUnit.io.memA.writeEnable
  io.memB.address := memUnit.io.memB.address
  io.memB.in := memUnit.io.memB.in
  io.memB.writeEnable := memUnit.io.memB.writeEnable

  memUnit.io.memA.out := io.memA.out
  memUnit.io.memB.out := io.memB.out

  idwbUnit.io.wbIn := memUnit.io.wbOut

  io.finishFlag := idwbUnit.io.finishFlag
  io.regOut := idwbUnit.io.regOut


  io.testRegWrite := memUnit.io.wbOut.regWrite
  io.testRegWriteEnable := memUnit.io.wbOut.regWriteEnable
  io.testRegWriteData := memUnit.io.wbOut.regWriteData

  if(conf.load){
    io.testClockIF := !io.load
    ifUnit.io.enable := (!io.load)&&(!idwbUnit.io.stole)
    idwbUnit.io.idEnable := !io.load
    exUnit.io.enable := !io.load
    memUnit.io.enable := !io.load
    idwbUnit.io.wbEnable := !io.load
    io.memA.load := io.load
    io.memB.load := io.load
  }else{
    io.testClockIF := ifUnit.io.enable
    ifUnit.io.enable := !idwbUnit.io.stole
    idwbUnit.io.idEnable := true.B
    exUnit.io.enable := true.B
    memUnit.io.enable := true.B
    idwbUnit.io.wbEnable := true.B
    io.memA.load := false.B
    io.memB.load := false.B
  }
}
