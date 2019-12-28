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
          step(1)
          expect(c.io.exOut.instAALU.opcode, 0)
          expect(c.io.exOut.instBALU.opcode, 0)
          expect(c.io.memOut.instAMemRead, false)
          expect(c.io.memOut.instBMemRead, false)
          expect(c.io.wbOut.instARegWrite.regWriteEnable, true)
          expect(c.io.wbOut.instBRegWrite.regWriteEnable, true)
        }
    } should be (true)
  }
}
