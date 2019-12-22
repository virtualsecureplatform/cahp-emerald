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
  assert(Driver(() => new IfUnit) {
    c =>
      new PeekPokeTester(c) {
        poke(c.io.in.romData, 0x01020304)
        poke(c.io.in.jumpAddress, 0)
        poke(c.io.in.jump, false)
        poke(c.io.enable, true)
        expect(c.io.testRomCacheState, romCacheStateType.NotLoaded)
        expect(c.io.out.instOut, 0x030201)
        expect(c.io.out.romAddress, 0)
        expect(c.io.out.stole, false)
        step(1)
        poke(c.io.in.romData, 0x05060708)
        expect(c.io.testRomCacheState, romCacheStateType.Loaded)
        expect(c.io.testRomCache, 0x01020304)
        expect(c.io.out.pcAddress, 3)
        expect(c.io.out.romAddress, 1)
        expect(c.io.out.instOut, 0x060504)
        expect(c.io.out.stole, false)
        poke(c.io.in.jumpAddress, 0)
        poke(c.io.in.jump, true)
        step(1)
        poke(c.io.in.romData, 0x01020304)
        expect(c.io.testRomCacheState, romCacheStateType.NotLoaded)
        expect(c.io.out.instOut, 0x030201)
        expect(c.io.out.romAddress, 0)
        expect(c.io.out.stole, false)
        poke(c.io.in.jumpAddress, 2)
        step(1)
        poke(c.io.in.romData, 0x01020304)
        poke(c.io.in.jump, false)
        expect(c.io.testRomCacheState, romCacheStateType.NotLoaded)
        expect(c.io.out.romAddress, 0)
        expect(c.io.out.stole, true)
        step(1)
        poke(c.io.in.romData, 0x05060708)
        poke(c.io.testRomCacheState, romCacheStateType.Loaded)
        expect(c.io.testRomCache, 0x01020304)
        expect(c.io.out.romAddress, 1)
        expect(c.io.out.instOut, 0x050403)
        expect(c.io.out.stole, false)
      }
  })
}

