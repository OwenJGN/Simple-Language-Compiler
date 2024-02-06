.macro    PushImm     $number
    li            t1, $number
    sw           t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    PushRel     $offset
    lw            t1, $offset(fp)
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    PopRel      $offset
    lw            t1, 4(sp)
    addi          sp, sp, 4
    sw            t1, $offset(fp)
.end_macro

.macro    Reserve     $bytes
    addi          sp, sp, -$bytes
.end_macro

.macro    Discard     $bytes
    addi          sp, sp, $bytes
.end_macro

.macro    SetFP
    mv            fp, sp
.end_macro

.macro    SaveFP
    sw            fp, (sp)
    addi          sp, sp, -4
.end_macro

.macro    RestoreFP
    lw            fp, 4(sp)
    addi          sp, sp, 4
.end_macro

.macro    Popt1t2
    lw            t1, 4(sp)
    addi          sp, sp, 4
    lw            t2, 4(sp)
    addi          sp, sp, 4
.end_macro

.macro    CompGT
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    bgt           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    CompGE
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    bge           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    CompEq
    Popt1t2
    li            t0, 1
    sw            t0, (sp)
    beq           t1, t2, exit
    sw            zero, (sp)
exit:
    addi          sp, sp, -4
.end_macro

.macro    Invert
    lw            t1, 4(sp)
    li            t0, 1
    sw            t0, 4(sp)
    beqz          t1, exit
    sw            zero, 4(sp)
exit:
.end_macro

.macro    Plus
    Popt1t2
    add           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Minus
    Popt1t2
    sub           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Times
    Popt1t2
    mul           t1, t1, t2
    sw            t1, (sp)
    addi          sp, sp, -4
.end_macro

.macro    Jump        $address
    j            $address
.end_macro

.macro    JumpTrue    $address
    lw            t1, 4(sp)
    addi          sp, sp, 4
    beqz          t1, exit
    j             $address
exit:
.end_macro

.macro    Invoke      $address
    jal           next
next:
    mv            t1, ra
    addi          t1, t1, 20
    sw            t1, (sp)
    addi          sp, sp, -4
    j             $address
.end_macro

.macro    Return      $bytes
    lw            t1, 4(sp)
    addi          sp, sp, 4
    addi          sp, sp, $bytes
    jr            t1
.end_macro

.macro    Print
    li            a7, 1
    lw            a0, 4(sp)
    addi          sp, sp, 4
    ecall
.end_macro

.macro    PrintSpace
    li            a7, 11
    li            a0, 32
    ecall
.end_macro


.text

# bootstrap loader that runs main()

boot:

    PushImm     0       # return value

    Invoke      main
    lw          a0, 4(sp)
    addi        sp, sp, 4
    li          a7, 10
    ecall
main:
    SaveFP
    SetFP
    PushImm     0       # return value
    PushImm     10
    Invoke      fibo
    PopRel      12
    RestoreFP
    Return      0
fibo:
    SaveFP
    SetFP
    PushImm     2
    PushRel     (12)
    CompGE
    Invert
    Invert
    JumpTrue    label_0
    PushRel     (12)
    Jump        label_1
label_0:
    PushImm     0       # return value
    PushImm     2
    PushRel     (12)
    Minus
    Invoke      fibo
    PushImm     0       # return value
    PushImm     1
    PushRel     (12)
    Minus
    Invoke      fibo
    Plus
label_1:
    PopRel      16
    RestoreFP
    Return      4