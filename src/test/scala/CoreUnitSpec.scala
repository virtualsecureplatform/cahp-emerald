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

import scala.util.control.Breaks

class CoreUnitSpec() extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = true
  conf.test = true
  conf.load = true

  val testDir = new File("src/test/binary/")

  testDir.listFiles().foreach { f =>
    if(f.getName().contains("0004-lisp.bin")) {
      println(f.getName())
      val parser = new TestBinParser(f.getAbsolutePath())
      println(parser.romSeq)
      conf.testRom = parser.romSeq

      val cycle = parser.cycle
      var cycleFinishFlag = false
      /*
      assert(Driver(() => new TopUnit(parser.memASeq, parser.memBSeq)) {
        c =>
          new PeekPokeTester(c) {
            poke(c.io.load, true)
            for (i <- 0 until 255){
              step(1)
            }
            poke(c.io.load, false)
            val b = new Breaks;
            b.breakable{
              for (i <- 0 until cycle) {
                step(1)
                if(peek(c.io.finishFlag) == 1){
                  println("FINISH");
                  b.break;
                }
              }
            }
            expect(c.io.testRegx8, parser.res)
          }
      })
       */
    }
  }
}

