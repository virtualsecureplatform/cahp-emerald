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
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = true
  conf.debugWb = true
  conf.test = true


  val testDir = new File("src/test/binary/")
  testDir.listFiles().foreach{f =>
    if(f.getName().contains(".bin")){
      println(f.getName())
      val parser = new TestBinParser(f.getAbsolutePath())
      conf.testRom = parser.romSeq
      println(parser.romSeq)
      assert(Driver(() => new TopUnit()) {
        c =>
          new PeekPokeTester(c) {
            for (i <- 0 until parser.cycle) {
              step(1)
            }
            expect(c.io.regOut.x8, parser.res)
            expect(c.io.finishFlag, true)
          }

      })
    }
  }
}

