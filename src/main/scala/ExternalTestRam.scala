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

class ExternalTestRam(val initContent:Seq[BigInt])(implicit val conf:CAHPConfig) extends Module{
  val io = IO(new MemPort(conf))
  val mem = Mem(256, UInt(8.W))
  val pReg = RegInit(0.U.asTypeOf(new MemPort(conf)))

  val loadAddr = RegInit(0.U(8.W))
  val loadRam = VecInit(initContent map (x=> x.U(8.W)))


  pReg.address := io.address
  pReg.in := io.in
  pReg.writeEnable := io.writeEnable

  if(conf.load) {
    when(conf.load.B && (io.load === 1.U)) {
      mem(loadAddr) := loadRam(loadAddr)
      loadAddr := loadAddr + 1.U
      io.out := loadRam(loadAddr)
      //printf("[MEM INIT] MemWrite Mem[0x%x] <= Data:0x%x\n", loadAddr, loadRam(loadAddr))
    }.otherwise {
      when(pReg.writeEnable) {
        mem(pReg.address) := pReg.in
        when(conf.debugMem.B) {
          printf("[MEM] MemWrite Mem[0x%x] <= Data:0x%x\n", pReg.address, pReg.in)
        }
        io.out := pReg.in
      }.otherwise {
        io.out := mem(pReg.address)
      }
    }
  }else{
    when(pReg.writeEnable) {
      mem(pReg.address) := pReg.in
      when(conf.debugMem.B) {
        printf("[MEM] MemWrite Mem[0x%x] <= Data:0x%x\n", pReg.address, pReg.in)
      }
      io.out := pReg.in
    }.otherwise {
      io.out := mem(pReg.address)
    }
  }
}
