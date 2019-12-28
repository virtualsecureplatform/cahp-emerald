import java.io.File

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class TestUnitSpec extends ChiselFlatSpec{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = false

  conf.test = true

  val testFile = new File("utils/test.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array(""), () => new TestUnit){
      c =>
        new PeekPokeTester(c) {
          expect(c.io.instAOut, TestUtils.genADD(0, 1, 2))
          expect(c.io.instBOut, TestUtils.genADD(1, 2, 3))
          expect(c.io.execB, true)
          step(1)
          expect(c.io.instAOut, TestUtils.genADD(2, 3, 4))
          expect(c.io.instBOut, TestUtils.genADD(3, 4, 5))
          expect(c.io.execB, true)

        }
    } should be (true)
  }
}
