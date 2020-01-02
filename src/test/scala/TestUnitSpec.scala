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

  val testFile = new File("src/test/binary/addi-1.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array(""), () => new TestUnit){
      c =>
        new PeekPokeTester(c) {
          expect(c.io.ifOut.instAOut, TestUtils.genLI(1, 0x20))
          expect(c.io.ifOut.instBOut, 0)
          step(1)
          expect(c.io.ifOut.instAOut, TestUtils.genADDI(8,1,0x0A))
          val instB = peek(c.io.ifOut.instBOut)&0xFFFF
          assert(instB == TestUtils.genJS(0))
          expect(c.io.ifOut.pcAddress, 6)
          step(1)
          //ID
          step(1)
          //EX
          expect(c.io.exUnitOut.jumpAddress, 6)
          expect(c.io.exUnitOut.jump, true)
          val instA = peek(c.io.ifOut.instAOut)&0xFFFF
          assert(instA == TestUtils.genJS(0))
          expect(c.io.ifOut.pcAddress, 6)
          step(1)
          expect(c.io.exOut.bcIn.pc, 6)
          step(1)
          expect(c.io.exUnitOut.jumpAddress, 6)
          expect(c.io.exUnitOut.jump, true)

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
