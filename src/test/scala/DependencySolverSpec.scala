import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class DependencySolverSpec extends ChiselFlatSpec {
  implicit val conf = CAHPConfig()

  behavior of "DependencySolver Test"
    it should "Do not Execute dependent instructions" in {
      Driver.execute(Array(""), () => new DependencySolver) {
        c =>
          new PeekPokeTester(c) {
            poke(c.io.instA, TestUtils.genJS(10))
            poke(c.io.instB, TestUtils.genADD(1, 2, 3))
            expect(c.io.execB, false)

            poke(c.io.instA, TestUtils.genLW(0,0,0))
            poke(c.io.instB, TestUtils.genLW(1,1,0))
            expect(c.io.execB, false)

            poke(c.io.instA, TestUtils.genLW(0,0,0))
            poke(c.io.instB, TestUtils.genADD(2,1,0))
            expect(c.io.execB, false)

            poke(c.io.instA, TestUtils.genLW(0,1,0))

            poke(c.io.instB, TestUtils.genADD(1,1,1))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genADD(1,1,0))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genADDI(1, 2, 1))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genADDI(1, 0, 1))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genBEQ(1, 2, 0))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genBEQ(0, 1, 0))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genADD2(1, 2))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genADD2(0, 1))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genADDI2(1, 2))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genADDI2(0, 1))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genJALR(1))
            expect(c.io.execB, true)
            poke(c.io.instB, TestUtils.genJALR(0))
            expect(c.io.execB, false)
            poke(c.io.instB, TestUtils.genJS(1))
            expect(c.io.execB, true)
          }
      } should be (true)
    }
}
