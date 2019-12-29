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
          expect(c.io.ifOut.instAOut, TestUtils.genLI(0, 1))
          expect(c.io.ifOut.instBOut, TestUtils.genLI(1, 2))
          expect(c.io.ifOut.pcAddress, 3)
          step(1)
          expect(c.io.ifOut.instAOut, TestUtils.genADD(2, 0, 1))
          expect(c.io.ifOut.instBOut, TestUtils.genADD(3, 1, 1))
          expect(c.io.ifOut.pcAddress, 9)
          expect(c.io.exOut.instAALU.opcode, 8)
          expect(c.io.exOut.instBALU.opcode, 8)
          expect(c.io.exOut.instAALU.inB, 1)
          expect(c.io.exOut.instBALU.inB, 2)
          expect(c.io.exOut.selJump, false)
          expect(c.io.exOut.selMem, false)
          expect(c.io.memOut.instAMemRead, false)
          expect(c.io.memOut.instBMemRead, false)
          expect(c.io.wbOut.instARegWrite.regWrite, 0)
          expect(c.io.wbOut.instBRegWrite.regWrite, 1)
          expect(c.io.wbOut.instARegWrite.regWriteEnable, true)
          expect(c.io.wbOut.instBRegWrite.regWriteEnable, true)
          step(1)
          expect(c.io.exUnitOut.instARes, 1)
          expect(c.io.exUnitOut.instBRes, 2)
          val instJS = peek(c.io.ifOut.instAOut)
          assert((instJS&0xFFFF) == TestUtils.genJS(-12))
          expect(c.io.ifOut.pcAddress, 0xc)
          expect(c.io.ifOut.instBOut, 0)
          step(1)
          expect(c.io.memWbOut.instARegWrite.regWrite, 0)
          expect(c.io.memWbOut.instARegWrite.regWriteEnable, true)
          expect(c.io.memWbOut.instARegWrite.regWriteData, 1)
          expect(c.io.memWbOut.instBRegWrite.regWrite, 1)
          expect(c.io.memWbOut.instBRegWrite.regWriteEnable, true)
          expect(c.io.memWbOut.instBRegWrite.regWriteData, 2)
          step(1)
          expect(c.io.exUnitOut.jump, true)
          step(1)
          expect(c.io.memWbOut.instBRegWrite.regWriteEnable, false)
          step(1)
          step(10)
        }
    } should be (true)
  }
}
