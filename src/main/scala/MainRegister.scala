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

class MainRegisterPort(implicit val conf:CAHPConfig) extends Bundle {
  val rs1 = Input(UInt(4.W))
  val rs2 = Input(UInt(4.W))
  val rd = Input(UInt(4.W))
  val writeEnable = Input(Bool())
  val writeData = Input(UInt(16.W))

  val rs1Data = Output(UInt(16.W))
  val rs2Data = Output(UInt(16.W))

  val testRegx8 = if (conf.test) Output(UInt(16.W)) else Output(UInt(0.W))
  val testPC = if(conf.test) Input(UInt(9.W)) else Input(UInt(0.W))

  val regOut = new MainRegisterOutPort
}

class MainRegister(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new MainRegisterPort)

  val MainReg = Mem(16, UInt(16.W))

  io.rs1Data := MainReg(io.rs1)
  io.rs2Data := MainReg(io.rs2)
  io.testRegx8 := MainReg(8)

  when(io.writeEnable) {
    MainReg(io.rd) := io.writeData
    when(conf.debugWb.B) {
      printf("%x Reg x%d <= 0x%x\n", io.testPC, io.rd, io.writeData)
    }
  }

  io.regOut.x0 := MainReg(0)
  io.regOut.x1 := MainReg(1)
  io.regOut.x2 := MainReg(2)
  io.regOut.x3 := MainReg(3)
  io.regOut.x4 := MainReg(4)
  io.regOut.x5 := MainReg(5)
  io.regOut.x6 := MainReg(6)
  io.regOut.x7 := MainReg(7)
  io.regOut.x8 := MainReg(8)
  io.regOut.x9 := MainReg(9)
  io.regOut.x10 := MainReg(10)
  io.regOut.x11 := MainReg(11)
  io.regOut.x12 := MainReg(12)
  io.regOut.x13 := MainReg(13)
  io.regOut.x14 := MainReg(14)
  io.regOut.x15 := MainReg(15)
}
