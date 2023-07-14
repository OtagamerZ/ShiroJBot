grammar ShoukanExpr;

expr: sumsub;

sumsub
    : muldiv ' '? '+' ' '? sumsub   # Addition
    | muldiv ' '? '-' ' '? sumsub   # Subtraction
    | muldiv                        # Operation
    ;

muldiv
    : group ' '? '*' ' '? muldiv    # Product
    | group ' '? '/' ' '? muldiv    # Division
    | group                         # Brackets
    ;

group
    : WORD? '(' sumsub ')'          # Function
    | var                           # Variable
    ;

var
    : '$' WORD ('.' WORD)?
    | NUM
    ;

NUM: [0-9]+ ('.' [0-9]+)?;
WORD: [a-z]+;
OPERATOR_SS: ('+'|'-');
OPERATOR_MD: ('*'|'/');