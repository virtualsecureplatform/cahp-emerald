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

import java.io.File

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class TopUnitSpec() extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = true
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  conf.test = true


  val parser = new TestBinParser("src/test/binary/fib.bin")
  conf.testRom = parser.romSeq
  println(parser.romSeq)
  assert(Driver(() => new YosysTest2(parser.memASeq, parser.memBSeq)) {
    c =>
      new PeekPokeTester(c) {
        for (i <- 0 until 60) {
          step(1)
        }
        expect(c.io.testRegx8, 55)
      }

  })
}

