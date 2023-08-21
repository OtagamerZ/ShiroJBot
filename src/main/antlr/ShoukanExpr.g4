grammar ShoukanExpr;

line: expr;

expr: left=expr op=(MUL|DIV) right=expr               #MulDiv
    | left=expr op=(SUM|SUB) right=expr               #SumSub
    | func=(MIN|MAX) '(' left=expr ',' right=expr ')' #Function
    | SUB? '(' expr ')'                               #Group
    | SUB? element=(VAR|NUM) ntype=('d'|'f')?         #Value
    ;

MUL: '*';
DIV: '/';
SUM: '+';
SUB: '-';
MIN: 'min';
MAX: 'max';
NUM: [0-9]+ ('.' [0-9]+)?;
NAME: [a-z]+;
VAR: '$' NAME ('.' NAME)?;
