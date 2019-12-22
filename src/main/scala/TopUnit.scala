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

class TopUnitPort(implicit val conf:CAHPConfig) extends Bundle {
  val load = if(conf.load) Input(Bool()) else Input(UInt(0.W))
  val finishFlag = Output(Bool())
  val testRegx8 = if (conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
}

class TopUnit(val memAInit:Seq[BigInt], val memBInit:Seq[BigInt])(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new TopUnitPort)
  val core = Module(new CoreUnit)
  val rom = Module(new ExternalTestRom())
  val memA = Module(new ExternalTestRam(memAInit))
  val memB = Module(new ExternalTestRam(memBInit))

  rom.io.romAddress := core.io.romAddr
  core.io.romData := rom.io.romData
  core.io.load := io.load


  memA.io.load := io.load
  memA.io.address := core.io.memA.address
  memA.io.in := core.io.memA.in
  memA.io.writeEnable := core.io.memA.writeEnable
  core.io.memA.out := memA.io.out

  memB.io.load := io.load
  memB.io.address := core.io.memB.address
  memB.io.in := core.io.memB.in
  memB.io.writeEnable := core.io.memB.writeEnable
  core.io.memB.out := memB.io.out

  io.testRegx8 := core.io.testRegx8
  io.finishFlag := core.io.finishFlag
}
