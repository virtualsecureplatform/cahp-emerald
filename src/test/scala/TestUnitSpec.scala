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

  val testFile = new File("src/test/binary/beq-2.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array(""), () => new TestUnit){
      c =>
        new PeekPokeTester(c) {
          expect(c.io.ifOut.instAOut, TestUtils.genLI(1, 2))
          expect(c.io.ifOut.instBOut, TestUtils.genLI(2, 1))
          step(1)
          expect(c.io.ifOut.instAOut, TestUtils.genBEQ(1,2,8))
          expect(c.io.ifOut.instBOut, 0)
          step(1)
          expect(c.io.exOut.instAALU.inA, 2)
          expect(c.io.exOut.instAALU.inB, 1)
          expect(c.io.exOut.bcIn.pcOpcode, 1)
          expect(c.io.exOut.instAALU.opcode, 1)
          expect(c.io.exOut.selJump, false)
          step(1)
          expect(c.io.exUnitOut.jump, false)
          expect(c.io.exUnitOut.instARes, 1)
          /*
          expect(c.io.ifOut.instAOut, TestUtils.genLI(4, 1))
          expect(c.io.ifOut.instBOut, TestUtils.genLI(5, 2))
          step(1)
          val instA1 = peek(c.io.ifOut.instAOut)&0xFFFF
          val instB1 = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instA1 == TestUtils.genADD2(2, 4))
          assert(instB1 == TestUtils.genADD2(3, 5))
           */
          step(100)
          expect(c.io.wbOut.finishFlag, true)
        }
    } should be (true)
  }
}
