object TestUtils {
  def genSimm11(imm:Int):Int = {
    if(imm > 0){
      imm&0x3FF
    }else{
      (imm&0x3FF)|0x400
    }
  }

  def genSimm10(imm:Int):Int = {
    if(imm > 0){
      imm&0x1FF
    }else{
      (imm&0x1FF)|0x200
    }
  }

  def genSimm6(imm:Int):Int = {
    if(imm > 0){
      imm&0x1F
    }else{
      (imm&0x1F)|0x20
    }
  }

  def genUimm7(imm:Int):Int = {
    imm&0x7F
  }

  def genUimm4(imm:Int):Int = {
    imm&0xF
  }

  def genLW(rd:Int, rs:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("010101",2)
  }

  def genLB(rd:Int, rs:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("100101",2)
  }

  def genLBU(rd:Int, rs:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("000101",2)
  }

  def genSW(rd:Int, rs:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rd << 12)|(rs << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("011101",2)
  }

  def genSB(rd:Int, rs:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rd << 12)|(rs << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("110101",2)
  }

  def genLI(rd:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rd << 12)|((imm>>8)<<6)|Integer.parseUnsignedInt("110101",2)
  }

  def genADD(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00000001", 2)
  }

  def genSUB(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00001001", 2)
  }

  def genAND(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00010001", 2)
  }

  def genXOR(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00011001", 2)
  }

  def genOR(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00100001", 2)
  }

  def genLSL(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00101001", 2)
  }

  def genLSR(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00110001", 2)
  }

  def genASR(rd:Int, rs1:Int, rs2:Int):BigInt = {
    (rs2<<16)|(rs1<<12)|(rd<<8)|Integer.parseUnsignedInt("00111001", 2)
  }

  def genADDI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("000011",2)
  }

  def genANDI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("010011",2)
  }

  def genXORI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("011011",2)
  }

  def genORI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rd << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("100011",2)
  }

  def genLSLI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    ((uimm4&0xF)<<16)|(rs1 << 12)|(rd << 8)|Integer.parseUnsignedInt("101011",2)
  }

  def genLSRI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    ((uimm4&0xF)<<16)|(rs1 << 12)|(rd << 8)|Integer.parseUnsignedInt("110011",2)
  }

  def genASRI(rd:Int, rs1:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    ((uimm4&0xF)<<16)|(rs1 << 12)|(rd << 8)|Integer.parseUnsignedInt("111011",2)
  }

  def genBEQ(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("001111",2)
  }

  def genBNE(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("101111",2)
  }

  def genBLT(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("110111",2)
  }

  def genBLTU(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("010111",2)
  }

  def genBLE(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("111111",2)
  }

  def genBLEU(rs1:Int, rs2:Int, imm:Int):BigInt = {
    val simm10 = genSimm10(imm)
    ((simm10&0xFF)<<16)|(rs1 << 12)|(rs2 << 8)|((imm>>8)<<6)|Integer.parseUnsignedInt("011111",2)
  }

  def genLWSP(rd:Int, imm:Int):BigInt = {
    val uimm7 = genUimm7(imm)>>1
    ((uimm7&0xF)<<12)|(rd<<8)|((uimm7>>4)<<6)|Integer.parseUnsignedInt("010100", 2)
  }

  def genSWSP(rs:Int, imm:Int):BigInt = {
    val uimm7 = genUimm7(imm)>>1
    ((uimm7&0xF)<<12)|(rs<<8)|((uimm7>>4)<<6)|Integer.parseUnsignedInt("011100", 2)
  }

  def genLSI(rd:Int, imm:Int):BigInt = {
    val simm6 = genSimm6(imm)
    ((simm6&0xF)<<12)|(rd<<8)|((simm6>>4)<<6)|Integer.parseUnsignedInt("110100", 2)
  }

  def genLUI(rd:Int, imm:Int):BigInt = {
    val simm6 = genSimm6(imm)
    ((simm6&0xF)<<12)|(rd<<8)|((simm6>>4)<<6)|Integer.parseUnsignedInt("000100", 2)
  }

  def genMOV(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("11000000", 2)
  }

  def genADD2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10000000", 2)
  }

  def genSUB2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10001000", 2)
  }

  def genAND2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10010000", 2)
  }

  def genXOR2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10011000", 2)
  }

  def genOR2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10100000", 2)
  }

  def genLSL2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10101000", 2)
  }

  def genLSR2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10110000", 2)
  }

  def genASR2(rd:Int, rs:Int):BigInt = {
    (rs<<12)|(rd<<8)|Integer.parseUnsignedInt("10111000", 2)
  }

  def genLSLI2(rd:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    (uimm4<<12)|(rd<<8)|Integer.parseUnsignedInt("00101010", 2)
  }

  def genLSRI2(rd:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    (uimm4<<12)|(rd<<8)|Integer.parseUnsignedInt("00110010", 2)
  }

  def genASRI2(rd:Int, imm:Int):BigInt = {
    val uimm4 = genUimm4(imm)
    (uimm4<<12)|(rd<<8)|Integer.parseUnsignedInt("00111010", 2)
  }

  def genADDI2(rd:Int, imm:Int):BigInt = {
    val simm6 = genSimm6(imm)
    ((simm6&0xF)<<12)|(rd<<8)|((simm6>>4)<<6)|Integer.parseUnsignedInt("000010", 2)
  }

  def genANDI2(rd:Int, imm:Int):BigInt = {
    val simm6 = genSimm6(imm)
    ((simm6&0xF)<<12)|(rd<<8)|((simm6>>4)<<6)|Integer.parseUnsignedInt("010010", 2)
  }

  def genJALR(rs:Int):BigInt = {
    (rs<<8)|Integer.parseUnsignedInt("10110", 2)
  }

  def genJR(rs:Int):BigInt = {
    (rs<<8)|Integer.parseUnsignedInt("00110", 2)
  }

  def genJS(imm:Int):BigInt = {
    val simm11 = genSimm11(imm)
    (simm11 << 5)|Integer.parseUnsignedInt("01110", 2)
  }

  def genJSAL(imm:Int):BigInt = {
    val simm11 = genSimm11(imm)
    (simm11 << 5)|Integer.parseUnsignedInt("11110", 2)
  }
}
