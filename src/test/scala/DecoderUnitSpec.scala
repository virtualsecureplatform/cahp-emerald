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

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class DecoderUnitSpec extends ChiselFlatSpec {
  val inst:Map[String, (Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int,Int)] = Map(
    //inst    inst,exop,mw,mr,mb,ms,ea,eb,ww,    im,pcim,pims,pcop
    "LW"  -> (0x15, 0x0, 0, 1, 0, 0, 0, 1, 1, 0x002, 0x4, 2, 0x0),
    "LB"  -> (0x25, 0x0, 0, 1, 1, 1, 0, 1, 1, 0x002, 0x4, 2, 0x0),
    "LBU" -> (0x05, 0x0, 0, 1, 1, 0, 0, 1, 1, 0x002, 0x4, 2, 0x0),
    "SW"  -> (0x1D, 0x0, 1, 0, 0, 2, 0, 1, 0, 0x002, 0x4, 2, 0x0),
    "SB"  -> (0x0D, 0x0, 1, 0, 1, 2, 0, 1, 0, 0x002, 0x4, 2, 0x0),
    "LI"  -> (0x35, 0x8, 0, 0, 2, 2, 2, 1, 1, 0x002, 0x4, 2, 0x0),
    "ADD" -> (0x01, 0x0, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "SUB" -> (0x09, 0x1, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "AND" -> (0x11, 0x2, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "XOR" -> (0x19, 0x3, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "OR"  -> (0x21, 0x4, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "LSL" -> (0x29, 0x5, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "LSR" -> (0x31, 0x6, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "ASR" -> (0x39, 0x7, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "ADDI"-> (0xC3, 0x0, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "ANDI"-> (0x53, 0x2, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "XORI"-> (0x5B, 0x3, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "ORI" -> (0x63, 0x4, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "LSLI"-> (0x2B, 0x5, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "LSRI"-> (0x33, 0x6, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "ASRI"-> (0x3B, 0x7, 0, 0, 2, 2, 0, 1, 1, 0x004, 0x4, 2, 0x0),
    "BEQ" -> (0x0F, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x1),
    "BNE" -> (0x2F, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x5),
    "BLT" -> (0x37, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x6),
    "BLTU"-> (0x17, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x2),
    "BLE" -> (0x3F, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x7),
    "BLEU"-> (0x1F, 0x1, 0, 2, 2, 2, 0, 0, 0, 0x200, 0x1, 1, 0x3),
    "LWSP"-> (0x14, 0x0, 0, 1, 0, 0, 0, 1, 1, 0x010, 0x4, 2, 0x0),
    "SWSP"-> (0x1C, 0x0, 1, 0, 0, 2, 0, 1, 0, 0x010, 0x4, 2, 0x0),
    "LSI" -> (0x34, 0x8, 0, 0, 2, 2, 2, 1, 1, 0x020, 0x4, 2, 0x0),
    "LUI" -> (0x04, 0x8, 0, 0, 2, 2, 2, 1, 1, 0x020, 0x4, 2, 0x0),
    "MOV" -> (0xC0, 0x8, 0, 0, 2, 2, 2, 0, 1, 0x200, 0x4, 2, 0x0),
    "ADD2"-> (0x80, 0x0, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "SUB2"-> (0x88, 0x1, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "AND2"-> (0x90, 0x2, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "XOR2"-> (0x98, 0x3, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "OR2" -> (0xA0, 0x4, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "LSL2"-> (0xA8, 0x5, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "LSR2"-> (0xB0, 0x6, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "ASR2"-> (0xB8, 0x7, 0, 0, 2, 2, 0, 0, 1, 0x200, 0x4, 2, 0x0),
    "LSLI2"->(0x2A, 0x5, 0, 0, 2, 2, 0, 1, 1, 0x040, 0x4, 2, 0x0),
    "LSRI2"-> (0x32,0x6, 0, 0, 2, 2, 0, 1, 1, 0x040, 0x4, 2, 0x0),
    "ASRI2"-> (0x3A,0x7, 0, 0, 2, 2, 0, 1, 1, 0x040, 0x4, 2, 0x0),
    "ADDI2"-> (0x02,0x0, 0, 0, 2, 2, 0, 1, 1, 0x020, 0x4, 2, 0x0),
    "ANDI2"-> (0x12,0x2, 0, 0, 2, 2, 0, 1, 1, 0x040, 0x4, 2, 0x0),
    "JALR"-> (0x16,0x00, 0, 2, 2, 2, 1, 1, 1, 0x100, 0x4, 0, 0x4),
    "JR"  -> (0x06,0x10, 0, 0, 2, 2, 2, 2, 0, 0x200, 0x4, 0, 0x4),
    "JS"  -> (0x0E,0x10, 0, 0, 2, 2, 2, 2, 0, 0x200, 0x2, 1, 0x4),
    "JSAL"-> (0x1E,0x00, 0, 2, 2, 2, 1, 1, 1, 0x100, 0x2, 1, 0x4),
    "NOP" -> (0x00,0x10, 0, 0, 2, 2, 2, 2, 0, 0x200, 0x4, 2, 0x0),
    )
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  conf.test = true
  assert(Driver(() => new Decoder()) {
    c =>
      new PeekPokeTester(c) {
        poke(c.io.in.pc, 0)
        inst.foreach{ item =>
          printf("Testing: %s\n", item._1)
          poke(c.io.in.inst, item._2._1)
          if(item._2._2 < 0x10) {
            expect(c.io.exOut.opcode, item._2._2)
          }
          expect(c.io.memOut.memWrite, item._2._3)
          if(item._2._4 < 2){
            expect(c.io.memOut.memRead, item._2._4)
          }
          if(item._2._5 < 2){
            expect(c.io.memOut.byteEnable, item._2._5)
          }
          if(item._2._6 < 2){
            expect(c.io.memOut.signExt, item._2._6)
          }
          if(item._2._7 < 2) {
            expect(c.io.inASel, item._2._7)
          }
          if(item._2._8 < 2) {
            expect(c.io.inBSel, item._2._8)
          }
          if(item._2._9 < 2) {
            expect(c.io.wbOut.regWriteEnable, item._2._9)
          }
          if(item._2._10 < 0x200) {
            expect(c.io.testImmType, item._2._10)
          }
          if(item._2._11 < 0x4) {
            expect(c.io.testPCImmType, item._2._11)
          }
          if(item._2._12 < 2) {
            expect(c.io.pcImmSel, item._2._12)
          }
          if(item._2._13 < 0x8) {
            expect(c.io.exOut.pcOpcode, item._2._13)
          }
        }
      }
  })
}

