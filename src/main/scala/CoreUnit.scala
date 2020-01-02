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
  val romData = Input(UInt(64.W))
  val romAddr = Output(UInt(conf.romAddrWidth.W))
  val memA = Flipped(new MemPort(conf))
  val memB = Flipped(new MemPort(conf))

  val finish = Output(Bool())
  val regOut = new MainRegisterOutPort()
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
  ifUnit.io.idStole := idwbUnit.io.stole
  ifUnit.io.enable := true.B

  idwbUnit.io.idIn.instA := ifUnit.io.out.instAOut
  idwbUnit.io.idIn.instB := ifUnit.io.out.instBOut
  idwbUnit.io.idIn.pc := ifUnit.io.out.pcAddress

  idwbUnit.io.exMemIn := exUnit.io.memOut
  idwbUnit.io.exWbIn := exUnit.io.wbOut
  idwbUnit.io.memWbIn := memUnit.io.wbOut
  idwbUnit.io.flush := false.B
  idwbUnit.io.idEnable := true.B
  idwbUnit.io.wbEnable := true.B


  exUnit.io.in     := idwbUnit.io.exOut
  exUnit.io.memIn  := idwbUnit.io.memOut
  exUnit.io.wbIn   := idwbUnit.io.wbOut
  exUnit.io.flush  := exUnit.io.out.jump
  exUnit.io.enable := true.B

  memUnit.io.in     := exUnit.io.memOut
  memUnit.io.wbIn   := exUnit.io.wbOut
  memUnit.io.memA.out := io.memA.out
  memUnit.io.memB.out := io.memB.out
  memUnit.io.enable := true.B
  io.memA.address := memUnit.io.memA.address
  io.memA.in := memUnit.io.memA.in
  io.memA.writeEnable := memUnit.io.memA.writeEnable
  io.memA.load := false.B
  io.memB.address := memUnit.io.memB.address
  io.memB.in := memUnit.io.memB.in
  io.memB.writeEnable := memUnit.io.memB.writeEnable
  io.memB.load := false.B

  idwbUnit.io.wbIn := memUnit.io.wbOut

  io.finish := idwbUnit.io.finishFlag
  io.regOut := idwbUnit.io.regOut
}
