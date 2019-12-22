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

import scala.util.Random

class MainRegisterSpec extends ChiselFlatSpec{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  conf.test = true
    assert(Driver(() => new MainRegister) {
      c =>
        new PeekPokeTester(c) {
          /*
          var testDataArray: Array[UInt] = Array.empty
          poke(c.io.rs, 0.U)
          poke(c.io.rd, 0.U)ホーム
          for(i <- 0 until 16){
            val v = Random.nextInt()&0xFFFF
            val test_i = v.asUInt(16.W)
            testDataArray = testDataArray :+ test_i
            poke(c.io.writeEnable, true.B)
            poke(c.io.writeReg, i.U(4.W))
            poke(c.io.writeData, test_i)
            step(1)
          }
          for(i <- 0 until 16){
            val v = Random.nextInt()&0xFFFF
            val test_i = v.asUInt(16.W)
            poke(c.io.writeEnable, false.B)
            poke(c.io.writeReg, i.U(4.W))
            poke(c.io.writeData, test_i)
            step(1)
          }
          for(i <- 0 until 16){
            poke(c.io.rs, i.U(4.W))
            expect(c.io.rsData, testDataArray(i))
          }
          for(i <- 0 until 16){
            poke(c.io.rd, i.U(4.W))
            expect(c.io.rdData, testDataArray(i))
          }
           */
        }
    })
}
