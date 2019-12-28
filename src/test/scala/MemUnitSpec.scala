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

class MemUnitSpec extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  assert(Driver(() => new MemUnitTest(Seq(BigInt(0)), Seq(BigInt(0)))) {
    c =>
      new PeekPokeTester(c) {
        poke(c.io.signExt, false)
        poke(c.io.byteEnable, false)
        poke(c.io.Enable, true)
        poke(c.io.instAMemRead, false)
        poke(c.io.memWrite, false)
        println("Pass through Test")
        for (i <- 0 until 100) {
          val v = Random.nextInt(0xFFFF)
          poke(c.io.address, v.U(16.W))
          poke(c.io.in, Random.nextInt(0xFFFF).U(16.W))
          step(1)
          expect(c.io.out, v.U(16.W))
        }

        println("Memory Word Write Test")
        var testDataArray: Array[UInt] = Array.empty
        poke(c.io.memWrite, true)
        for(i <- 0 until 100){
          val v = Random.nextInt(0xFFFF)
          poke(c.io.address, (i*2).U(9.W))
          poke(c.io.in, v.U(16.W))
          testDataArray = testDataArray :+ v.U(16.W)
          step(1)
        }
        poke(c.io.memWrite, false)
        poke(c.io.instAMemRead, true)
        for(i <- 0 until 100){
          poke(c.io.address, (i*2).U(9.W))
          step(1)
          expect(c.io.out, testDataArray(i))
        }
        poke(c.io.byteEnable, true)
        for(i <- 0 until 100){
          poke(c.io.address, (i*2).U(9.W))
          step(1)
          expect(c.io.out, testDataArray(i)(7,0))
          poke(c.io.address, ((i*2)+1).U(9.W))
          step(1)
          expect(c.io.out, testDataArray(i)(15,8))
        }
        testDataArray = Array.empty
        poke(c.io.instAMemRead, false)
        poke(c.io.memWrite, true)
        for(i <- 0 until 100){
          val v = Random.nextInt(0xFFFF)
          poke(c.io.address, i.U(9.W))
          poke(c.io.in, v.U(16.W))
          testDataArray = testDataArray :+ v.U(16.W)
          step(1)
        }
        poke(c.io.instAMemRead, true)
        poke(c.io.memWrite, false)
        for(i <- 0 until 100){
          poke(c.io.address, i.U(9.W))
          step(1)
          expect(c.io.out, testDataArray(i)(7,0))
        }
      }
  })
}

