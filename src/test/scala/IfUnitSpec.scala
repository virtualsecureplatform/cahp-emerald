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
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.util.Cat

class IfUnitSpec extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false

  conf.test = true
  behavior of "IfUnit Test"
  it should "Load instructions with prefetch" in {
    Driver.execute(Array(""), () => new IfUnit) {
      c =>
        new PeekPokeTester(c) {
          val rom: BigInt = TestUtils.genADD(0, 1, 2) | (TestUtils.genADD(1, 2, 3) << 24) | ((TestUtils.genADD(2, 3, 4) & 0xFFFF) << 48)
          poke(c.io.in.romData, rom)
          poke(c.io.in.jumpAddress, 0)
          poke(c.io.in.jump, false)
          poke(c.io.enable, true)
          expect(c.io.out.instAOut, TestUtils.genADD(0, 1, 2))
          expect(c.io.out.instBOut, TestUtils.genADD(1, 2, 3))
          expect(c.io.out.romAddress, 0)
          expect(c.io.out.pcAddress, 3)

          step(1)
          val rom2 = ((TestUtils.genADD(2, 3, 4) >> 16) & 0xFF) |
            (TestUtils.genADD2(3, 4) << 8) |
            (TestUtils.genADD(4, 5, 6) << 24) |
            (TestUtils.genADD2(4, 5) << 48)
          poke(c.io.in.romData, rom2)
          expect(c.io.out.pcAddress, 9.U)
          expect(c.io.out.romAddress, 1.U)
          expect(c.io.out.instAOut, TestUtils.genADD(2, 3, 4))
          var instB = peek(c.io.out.instBOut) & 0xFFFF
          assert(instB == TestUtils.genADD2(3, 4))

          step(1)
          val rom3 = TestUtils.genADD(5, 6, 7) |
            (TestUtils.genADD(6, 7, 0) << 24) |
            (TestUtils.genADD2(7, 0) << 48)
          poke(c.io.in.romData, rom3)
          expect(c.io.out.pcAddress, 11.U)
          expect(c.io.out.romAddress, 2.U)
          expect(c.io.out.instAOut, TestUtils.genADD(4, 5, 6))

          step(1)
          expect(c.io.out.pcAddress, 16.U)
          var instA = peek(c.io.out.instAOut) & 0xFFFF
          assert(instA == TestUtils.genADD2(4, 5))
          expect(c.io.out.instBOut, TestUtils.genADD(5, 6, 7))
        }
    } should be (true)
  }
}

