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
import chisel3.util.experimental.loadMemoryFromFile
import firrtl.annotations.MemoryLoadFileType

class ExternalRam(val load:Boolean, val fileName:String)(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new MemPort)
  val mem = Mem(256, UInt(8.W))

  when(io.in.writeEnable) {
    mem(io.in.address) := io.in.in
    when(conf.debugMem.B) {
      printf("[MEM] MemWrite Mem[0x%x] <= Data:0x%x\n", io.in.address, io.in.in)
    }
  }
  io.out := mem(io.in.address)
}
