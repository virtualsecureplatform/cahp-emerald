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

class InstRamPort extends Bundle {
  val address = Input(UInt(9.W))

  val out = Output(UInt(16.W))

}

class InstRam extends Module {
  val io = IO(new InstRamPort);

  val rom = Mem(256, UInt(16.W))

  io.out := rom(io.address(8,1))
}
