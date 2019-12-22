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
import chisel3.util._

class StateMachinePort extends Bundle {
  val clockIF = Output(Bool())
  val clockID = Output(Bool())
  val clockEX = Output(Bool())
  val clockMEM = Output(Bool())
  val clockWB = Output(Bool())
}

class StateMachine extends Module {
  val io = IO(new StateMachinePort)

  val state = RegInit(2.U(5.W))
  state := Cat(state, state(4))

  io.clockIF := true.B
  io.clockID := true.B
  io.clockEX := true.B
  io.clockMEM := true.B
  io.clockWB := true.B
  /*
  io.clockIF := state(0)
  io.clockID := state(1)
  io.clockEX := state(2)
  io.clockMEM := state(3)
  io.clockWB := state(4)
   */
}