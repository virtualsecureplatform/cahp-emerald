import java.io.File

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class TestUnitSpec extends ChiselFlatSpec{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = true
  conf.debugWb = true

  conf.test = true

  val testFile = new File("src/test/binary/swsp-1.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array(""), () => new TestUnit){
      c =>
        new PeekPokeTester(c) {
          expect(c.io.ifOut.instAOut, TestUtils.genLI(1, 4))
          expect(c.io.ifOut.instBOut, TestUtils.genLI(0, 0x2A))
          step(1)
          val instA = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA == TestUtils.genSWSP(0, 2))
          expect(c.io.ifOut.instBOut, 0)
          step(2)
          step(100)
          expect(c.io.wbOut.finishFlag, true)
        }
    } should be (true)
  }
}
