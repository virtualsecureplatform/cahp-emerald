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
          step(1)
          step(1800)
          expect(c.io.wbOut.finishFlag, true)
        }
    } should be (true)
  }
}
