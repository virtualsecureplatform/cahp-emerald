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

  behavior of "MainRegister Test"
  it should "Save and Load same value" in {
    Driver.execute(Array("--top-name=MainRegister"), () => new MainRegister){
      c => new PeekPokeTester(c) {
        poke(c.io.portA.writeEnable, false)
        poke(c.io.portB.writeEnable, false)

        //Test portA save and load
        for(i <-0 until 16){
          val testValue = Random.nextInt()&0xFFFF
          poke(c.io.portA.writeEnable, true)
          poke(c.io.portA.rd, i)
          poke(c.io.portA.writeData, testValue)
          step(1)
          poke(c.io.portA.rs1, i)
          poke(c.io.portA.rs2, i)
          expect(c.io.portA.rs1Data, testValue)
          expect(c.io.portA.rs2Data, testValue)
        }
        poke(c.io.portA.writeEnable, false)

        //Test portB save and load
        for(i <-0 until 16){
          val testValue = Random.nextInt()&0xFFFF
          poke(c.io.portB.writeEnable, true)
          poke(c.io.portB.rd, i)
          poke(c.io.portB.writeData, testValue)
          step(1)
          poke(c.io.portB.rs1, i)
          poke(c.io.portB.rs2, i)
          expect(c.io.portB.rs1Data, testValue)
          expect(c.io.portB.rs2Data, testValue)
        }
        poke(c.io.portB.writeEnable, false)

        //Test save via portA and load via portB
        for(i <-0 until 16){
          val testValue = Random.nextInt()&0xFFFF
          poke(c.io.portA.writeEnable, true)
          poke(c.io.portA.rd, i)
          poke(c.io.portA.writeData, testValue)
          step(1)
          poke(c.io.portB.rs1, i)
          poke(c.io.portB.rs2, i)
          expect(c.io.portB.rs1Data, testValue)
          expect(c.io.portB.rs2Data, testValue)
        }

        //Test save via portB and load via portA
        for(i <-0 until 16){
          val testValue = Random.nextInt()&0xFFFF
          poke(c.io.portB.writeEnable, true)
          poke(c.io.portB.rd, i)
          poke(c.io.portB.writeData, testValue)
          step(1)
          poke(c.io.portA.rs1, i)
          poke(c.io.portA.rs2, i)
          expect(c.io.portA.rs1Data, testValue)
          expect(c.io.portA.rs2Data, testValue)
        }
      }
    } should be (true)
  }
}
