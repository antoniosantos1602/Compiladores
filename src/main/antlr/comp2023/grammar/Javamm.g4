grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : ([1-9][0-9]* | [0]) ;
ID : [a-zA-Z_$][a-zA-Z_0-9$]* ;

WS : [ \t\n\r\f]+ -> skip ;
COMMENTARY_LINE : '//' ~[\r\n]* -> skip ;
COMMENTARY_BLOCK : '/*' .*? '*/' -> skip ;

program
    : (importDeclaration)* classDeclaration  EOF
    ;

importDeclaration
    : 'import' importFrag ('.' importFrag)* ';' #Import
    ;

importFrag
    : id=ID
    ;


classDeclaration
    : 'class' name=ID ('extends' superclass=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' #Class
    ;

varDeclaration
    : type name=ID ';' #Field
    ;

methodDeclaration
    : ('public')? type methodName=ID '(' arguments ')' '{' (varDeclarationMethod)*(statement)* 'return' expression ';' '}' #Method
    | ('public')? 'static' returnType='void' methodName='main' '(' firstArgumentType=ID '[' ']' firstArgumentName=ID ')' '{' (varDeclarationMethod)*(statement)* '}' #MainMethod //Don't forget to make sure the first ID is String in the semantic analysis
    ;

varDeclarationMethod
    : type name=ID ';' #LocalVar
    ;

arguments
    : (argument (',' argument)*)?
    ;

argument
    : type name=ID
    ;

type
    : typeName='int' isArray='[' ']'
    | typeName='boolean'
    | typeName='int'
    | typeName=ID
    ;

statement
    : '{' (statement)* '}' #Block
    | 'if' '(' expression ')' statement 'else' statement #IfElse
    | 'while' '(' expression ')' statement #While
    | expression ';' #ExprStmt
    | var=ID '=' expression ';' #Assignment
    | var=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '(' expression ')' #ParenExpr
    | expression '[' expression ']' #ArrayAccess
    | expression '.' 'length' #ArrayLength
    | expression '.' methodName=ID '(' (expression (',' expression)*)? ')' #MethodCall
    | '!' expression #UniaryOp
    | 'new' 'int' '[' expression ']' #NewIntArray
    | 'new' className=ID '(' ')' #NewObject
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'this' #This
    | value=ID #Identifier
    | 'true' #True
    | 'false' #False
    | value=INTEGER #Integer
    ;


