import java.io.File

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class TestUnitSpec extends ChiselFlatSpec{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = true

  conf.test = true

  val testFile = new File("src/test/binary/0002-fib.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array(""), () => new TestUnit){
      c =>
        new PeekPokeTester(c) {
          expect(c.io.ifOut.instAOut, TestUtils.genLI(1, 510))
          val instB = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instB == TestUtils.genJSAL(48))
          step(1)
          val instA0 = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA0 == TestUtils.genJS(0))
          step(1)
          val instA = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA == TestUtils.genADDI2(1, -2))
          expect(c.io.ifOut.instBOut, 0)
          step(1)
          val instA3 = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA3 == TestUtils.genSWSP(0, 0))
          val instB3 = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instB3 == TestUtils.genLSI(8, 5))
          expect(c.io.ifOut.romCache, 0x34001CE1C2000661L)
          expect(c.io.ifOut.pcAddress, 0x37)
          step(45)
          expect(c.io.ifOut.instAOut, TestUtils.genBLE(8, 3, 7))
          step(1)
          val instA4 = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA4 == TestUtils.genMOV(8, 3))
          step(21)
          val instA5 = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instA5 == TestUtils.genADDI2(1, 6))
          step(1)
          val instA6 = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA6 == TestUtils.genJR(0))
          step(3)
          expect(c.io.exUnitOut.jump, true)
          expect(c.io.exUnitOut.jumpAddress, 0x27)
          expect(c.io.ifOut.pcAddress, 0x27)
          expect(c.io.ifOut.stole, true)
          step(1)
          expect(c.io.ifOut.pcAddress, 0x29)
          val instA7 = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA7 == TestUtils.genADD2(8, 4))
          val instB7 = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instB7 == TestUtils.genLWSP(4, 0))
          step(400)
        }
    } should be (true)
  }
}
