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


class ExUnitSpec extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false
  /*
  assert(Driver(() => new ExUnit) {
    c =>
      new PeekPokeTester(c) {
        poke(c.io.enable, true)
        for (i <- 0 until 100) {
          //ADD
          val a = Random.nextInt(0xFFFF)
          val b = Random.nextInt(0xFFFF)
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 0.U(3.W))
          step(1)
          val res = a + b
          expect(c.io.out.res, (res & 0xFFFF).asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //SUB
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0xFFFF
          val b_sub = ((~b)&0xFFFF) + 1
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 1.U(3.W))
          step(1)
          val res = a + b_sub
          expect(c.io.out.res, (res & 0xFFFF).asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //AND
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0xFFFF
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 2.U(3.W))
          step(1)
          val res = a & b
          expect(c.io.out.res, (res & 0xFFFF).asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //OR
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0xFFFF
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 4.U(3.W))
          step(1)
          val res = a | b
          expect(c.io.out.res, (res & 0xFFFF).asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //XOR
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0xFFFF
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 3.U(3.W))
          step(1)
          val res = a ^ b
          expect(c.io.out.res, (res & 0xFFFF).asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //LSL
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0x1F
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 5.U(3.W))
          step(1)
          val res = (a << b) & 0xFFFF
          expect(c.io.out.res, res.asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //LSR
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt() & 0x1F
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 6.U(3.W))
          step(1)
          val res = (a >> b) & 0xFFFF
          expect(c.io.out.res, res.asUInt(16.W))
        }
        for (i <- 0 until 100) {
          //ASR
          val a = Random.nextInt() & 0xFFFF
          val b = Random.nextInt(16)
          poke(c.io.in.inA, a.asUInt(16.W))
          poke(c.io.in.inB, b.asUInt(16.W))
          poke(c.io.in.opcode, 7.U(3.W))
          step(1)

          def shift_arithmetic(v: Int, shamt: Int): Int = {
            val sign_bit = (v >>> 15) & 0x1
            var mask = 0
            for (i <- 0 until shamt) {
              mask = mask | (sign_bit << 15 - i)
            }
            val res = v >>> shamt
            (res | mask) & 0xFFFF
          }

          val res = shift_arithmetic(a, b)
          expect(c.io.out.res, res.asUInt(16.W))
        }
      }
  })
   */
}
