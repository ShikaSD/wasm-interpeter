@file:Suppress("ConstPropertyName")

package me.shika.wasm.def

object WasmOpcodes {
    // Control
    const val Unreachable = 0x00
    const val NoOp = 0x01
    const val Block = 0x02
    const val Loop = 0x03
    const val Jump = 0x04
    const val If = 0x05
    const val Try = 0x06
    const val Catch = 0x07
    const val Throw = 0x08
    const val Rethrow = 0x09
    const val End = 0x0B
    const val Branch = 0x0C
    const val BranchIf = 0x0D
    const val BranchTable = 0x0E
    const val Return = 0x0F
    const val Call = 0x10
    const val CallIndirect = 0x11
    const val CallRef = 0x14
    const val ReturnCallRef = 0x15

    // Ref
    const val RefNull = 0xD0
    const val IsNull = 0xD1
    const val RefFunc = 0xD2
    const val RefEq = 0xD3
    const val RefAsNonNull = 0xD4
    const val StructNew = 0xD5
    const val StructNewDefault = 0xD6
    const val StructGet = 0xD7
    const val StructSet = 0xD8
    const val ArrayNew = 0xD9
    const val ArrayNewDefault = 0xDA
    const val ArrayNewFixed = 0xDB
    const val ArrayNewData = 0xDC
    const val ArrayNewElem = 0xDD
    const val ArrayGet = 0xDE
    const val ArraySet = 0xDF
    const val ArrayLen = 0xE0
    const val ArrayFill = 0xE1
    const val ArrayCopy = 0xE2
    const val ArrayInitData = 0xE3
    const val ArrayInitElem = 0xE4
    const val RefTest = 0xE5
    const val RefCast = 0xE6
    const val AnyConvert = 0xE7
    const val ExternConvert = 0xE8

    // Memory
    const val MemInit = 0xC5
    const val DataDrop = 0xC6
    const val MemCopy = 0xC7
    const val MemFill = 0xC8

    // Table
    const val TableInit = 0xC9
    const val ElemDrop = 0xCA
    const val TableCopy = 0xCB
    const val TableGrow = 0xCC
    const val TableSize = 0xCD
    const val TableFill = 0xCE

    // Parametric
    const val Drop = 0x1A
    const val Select = 0x1B
    const val SelectMany = 0x1C

    // Variable
    const val LocalGet = 0x20
    const val LocalSet = 0x21
    const val LocalTee = 0x22
    const val GlobalGet = 0x23
    const val GlobalSet = 0x24

    // Table
    const val TableGet = 0x25
    const val TableSet = 0x26

    // Memory
    const val MemLoadi32 = 0x28
    const val MemLoadi64 = 0x29
    const val MemLoadf32 = 0x2A
    const val MemLoadf64 = 0x2B
    const val MemLoadi32_8s = 0x2C
    const val MemLoadi32_8u = 0x2D
    const val MemLoadi32_16s = 0x2E
    const val MemLoadi32_16u = 0x2F
    const val MemLoadi64_8s = 0x30
    const val MemLoadi64_8u = 0x31
    const val MemLoadi64_16s = 0x32
    const val MemLoadi64_16u = 0x33
    const val MemLoadi64_32s = 0x34
    const val MemLoadi64_32u = 0x35
    const val MemStorei32 = 0x36
    const val MemStorei64 = 0x37
    const val MemStoref32 = 0x38
    const val MemStoref64 = 0x39
    const val MemStorei32_8 = 0x3A
    const val MemStorei32_16 = 0x3B
    const val MemStorei64_8 = 0x3C
    const val MemStorei64_16 = 0x3D
    const val MemStorei64_32 = 0x3E
    const val MemSize = 0x3F
    const val MemGrow = 0x40

    // Const
    const val i32_const = 0x41
    const val i64_const = 0x42
    const val f32_const = 0x43
    const val f64_const = 0x44

    // Int range
    const val i32_eqz = 0x45
    const val i64_eqz = 0x50
    const val i32_clz = 0x67
    const val i32_ctz = 0x68
    const val i32_popcnt = 0x69
    const val i64_clz = 0x79
    const val i64_ctz = 0x7A
    const val i64_popcnt = 0x7B
    const val f32_abs = 0x8B
    const val f32_neg = 0x8C
    const val f32_ceil = 0x8D
    const val f32_floor = 0x8E
    const val f32_trunc = 0x8F
    const val f32_nearest = 0x90
    const val f32_sqrt = 0x91
    const val f64_abs = 0x99
    const val f64_neg = 0x9A
    const val f64_ceil = 0x9B
    const val f64_floor = 0x9C
    const val f64_trunc = 0x9D
    const val f64_nearest = 0x9E
    const val f64_sqrt = 0x9F
    const val i32_wrap_i64 = 0xA7
    const val i32_trunc_f32_s = 0xA8
    const val i32_trunc_f32_u = 0xA9
    const val i32_trunc_f64_s = 0xAA
    const val i32_trunc_f64_u = 0xAB
    const val i64_extend_i32_s = 0xAC
    const val i64_extend_i32_u = 0xAD
    const val i64_trunc_f32_s = 0xAE
    const val i64_trunc_f32_u = 0xAF
    const val i64_trunc_f64_s = 0xB0
    const val i64_trunc_f64_u = 0xB1
    const val f32_convert_i32_s = 0xB2
    const val f32_convert_i32_u = 0xB3
    const val f32_convert_i64_s = 0xB4
    const val f32_convert_i64_u = 0xB5
    const val f32_demote_f64 = 0xB6
    const val f64_convert_i32_s = 0xB7
    const val f64_convert_i32_u = 0xB8
    const val f64_convert_i64_s = 0xB9
    const val f64_convert_i64_u = 0xBA
    const val f64_promote_f32 = 0xBB
    const val i32_reinterpret_f32 = 0xBC
    const val i64_reinterpret_f64 = 0xBD
    const val f32_reinterpret_i32 = 0xBE
    const val f64_reinterpret_i64 = 0xBF
    const val i32_extend8_s = 0xC0
    const val i32_extend16_s = 0xC1
    const val i64_extend8_s = 0xC2
    const val i64_extend16_s = 0xC3
    const val i64_extend32_s = 0xC4

    // Binary
    const val i32_eq = 0x46
    const val i32_ne = 0x47
    const val i32_lt_s = 0x48
    const val i32_lt_u = 0x49
    const val i32_gt_s = 0x4A
    const val i32_gt_u = 0x4B
    const val i32_le_s = 0x4C
    const val i32_le_u = 0x4D
    const val i32_ge_s = 0x4E
    const val i32_ge_u = 0x4F
    const val i64_eq = 0x51
    const val i64_ne = 0x52
    const val i64_lt_s = 0x53
    const val i64_lt_u = 0x54
    const val i64_gt_s = 0x55
    const val i64_gt_u = 0x56
    const val i64_le_s = 0x57
    const val i64_le_u = 0x58
    const val i64_ge_s = 0x59
    const val i64_ge_u = 0x5A
    const val f32_eq = 0x5B
    const val f32_ne = 0x5C
    const val f32_lt = 0x5D
    const val f32_gt = 0x5E
    const val f32_le = 0x5F
    const val f32_ge = 0x60
    const val f64_eq = 0x61
    const val f64_ne = 0x62
    const val f64_lt = 0x63
    const val f64_gt = 0x64
    const val f64_le = 0x65
    const val f64_ge = 0x66
    const val i32_add = 0x6A
    const val i32_sub = 0x6B
    const val i32_mul = 0x6C
    const val i32_div_s = 0x6D
    const val i32_div_u = 0x6E
    const val i32_rem_s = 0x6F
    const val i32_rem_u = 0x70
    const val i32_and = 0x71
    const val i32_or = 0x72
    const val i32_xor = 0x73
    const val i32_shl = 0x74
    const val i32_shr_s = 0x75
    const val i32_shr_u = 0x76
    const val i32_rotl = 0x77
    const val i32_rotr = 0x78
    const val i64_add = 0x7C
    const val i64_sub = 0x7D
    const val i64_mul = 0x7E
    const val i64_div_s = 0x7F
    const val i64_div_u = 0x80
    const val i64_rem_s = 0x81
    const val i64_rem_u = 0x82
    const val i64_and = 0x83
    const val i64_or = 0x84
    const val i64_xor = 0x85
    const val i64_shl = 0x86
    const val i64_shr_s = 0x87
    const val i64_shr_u = 0x88
    const val i64_rotl = 0x89
    const val i64_rotr = 0x8A
    const val f32_add = 0x92
    const val f32_sub = 0x93
    const val f32_mul = 0x94
    const val f32_div = 0x95
    const val f32_min = 0x96
    const val f32_max = 0x97
    const val f32_copysign = 0x98
    const val f64_add = 0xA0
    const val f64_sub = 0xA1
    const val f64_mul = 0xA2
    const val f64_div = 0xA3
    const val f64_min = 0xA4
    const val f64_max = 0xA5
    const val f64_copysign = 0xA6

    const val i32_trunc_sat_f32_s = 0xF0
    const val i32_trunc_sat_f32_u = 0xF1
    const val i32_trunc_sat_f64_s = 0xF2
    const val i32_trunc_sat_f64_u = 0xF3
    const val i64_trunc_sat_f32_s = 0xF4
    const val i64_trunc_sat_f32_u = 0xF5
    const val i64_trunc_sat_f64_s = 0xF6
    const val i64_trunc_sat_f64_u = 0xF7
}
