import java.io.File

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import scala.util.control.Breaks

class TestUnitSpec extends ChiselFlatSpec{
  implicit val conf = CAHPConfig()
  conf.debugIf = false
  conf.debugId = false
  conf.debugEx = false
  conf.debugMem = false
  conf.debugWb = true

  conf.test = true
  conf.load = true

  val testFile = new File("src/test/binary/0003-bf.bin")
  println(testFile.getName())
  val parser = new TestBinParser(testFile.getAbsolutePath())
  println(parser.romSeq)
  conf.testRom = parser.romSeq

  behavior of "TestUnit Test"

  it should "work well" in  {
    Driver.execute(Array("--backend-name=treadle"), () => new TestUnit(parser.memASeq, parser.memBSeq)){
      c =>
        new PeekPokeTester(c) {
          poke(c.io.load, true)
          for (i <- 0 until 255){
            step(1)
          }
          poke(c.io.load, false)
          step(44)
          step(1)
          step(1)
          expect(c.io.ifOut.instBOut, TestUtils.genLSLI(7, 8, 1))
          expect(c.io.ifOut.pcAddress, 0x3A)
          step(1)
          expect(c.io.ifOut.instAOut, TestUtils.genBEQ(6, 10, 166))
          expect(c.io.ifOut.pcAddress, 0x3d)
          /*
          val b = new Breaks
          b.breakable {
            for (i <- 0 to 230) {
              step(1)
              if (peek(c.io.ifOut.instAOut) == TestUtils.genBEQ(6, 10, 166)) {
                printf("CYCLE:%d\n", i)
                if(i > 40){
                  expect(c.io.ifOut.pcAddress, 0x3d)
                  b.break
                }
              }
            }
          }
          step(1)
          expect(c.io.ifOut.pcAddress, 0x40)
          expect(c.io.ifOut.instAOut, TestUtils.genADD(15, 6, 11))
          step(1)
          expect(c.io.wbOut.instARegWrite.regWrite, 15)
          expect(c.io.ifOut.instAOut, TestUtils.genLB(15, 15, 0))
          step(1)
          expect(c.io.exUnitOut.instARes, 3)
           */
        }
    } should be (true)
  }
}
