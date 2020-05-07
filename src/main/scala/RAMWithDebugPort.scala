import chisel3._

case class RAMConfig(){
  val memsize = 256
  val addrWidth = (scala.math.log10(memsize)/scala.math.log10(2)).toInt
  val width = 8
}
class RAMPortIn(implicit val conf:RAMConfig) extends Bundle {
  val in = UInt(conf.width.W)
  val address = UInt(conf.addrWidth.W)
  val writeEnable = Bool()
  val load = Bool()
  override def cloneType: this.type = new RAMPortIn().asInstanceOf[this.type]
}
class RAMPort(implicit val conf:RAMConfig) extends Bundle {
  val in = Input(new RAMPortIn)
  val out = Output(UInt(conf.width.W))
  val debug = Output(Vec(conf.memsize,UInt(conf.width.W)))
}
class RAMWithDebugPort(implicit val conf:RAMConfig) extends Module{
  val io = IO(new RAMPort)
  val mem = Mem(conf.memsize, UInt(conf.width.W))
  when(io.in.writeEnable) {
    mem(io.in.address) := io.in.in
  }
  io.out := mem(io.in.address)
  for(i <- 0 to conf.memsize-1){
    io.debug(i) := mem(i)
  }
}
