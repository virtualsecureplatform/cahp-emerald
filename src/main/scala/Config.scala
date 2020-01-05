case class CAHPConfig() {
  var debugIf = true
  var debugId = true
  var debugEx = true
  var debugMem = true
  var debugWb = true

  var test = false
  var load = false
  var memAHex = ""
  var memBHex = ""
  var testRom:Seq[BigInt] = Seq(BigInt(0))

  //IF Unit
  val romAddrWidth = 9
}
