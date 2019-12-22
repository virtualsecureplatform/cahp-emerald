import chisel3.{util, _}
import chisel3.util.Cat

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

class ExternalTestRomPort(implicit val conf:CAHPConfig) extends Bundle {
  val romAddress = Input(UInt((conf.romAddrWidth-2).W))
  val romData = Output(UInt((32.W)))
}
class ExternalTestRom(implicit val conf: CAHPConfig) extends Module {
  val io = IO(new ExternalTestRomPort)

  val rom = VecInit(conf.testRom map (x=> x.U(32.W)))
  io.romData := rom(io.romAddress)
}
