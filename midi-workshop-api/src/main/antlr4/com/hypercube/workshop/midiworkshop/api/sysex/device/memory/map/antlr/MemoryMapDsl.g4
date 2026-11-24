grammar MemoryMapDsl;

// ============================================================
//  .mmap  -  memory layout of a MIDI device
// ============================================================
//  The lexer only produces "words" and punctuation.
//  All interpretation (packed address, format, named dimensions,
//  hex size in the type field) happens in the visitor, not in
//  regular expressions.
//
//  Language rules:
//    - a name is a sequence of words (spaces allowed)
//    - a numeric token starts with a digit: "000100", "0C", "400000-000100"
//    - a text token starts with a letter or "_": "byte", "DS-330"
//    - "#" comments to the end of the line
//
//  The generated parser is named MemoryMapDslParser to avoid a
//  collision with MemoryMapParser (the facade).
// ============================================================

file
    : (NL | modelDecl | addressingDecl | zone | structDef | enumDef)* EOF
    ;

modelDecl
    : MODEL words
    ;

addressingDecl
    : PACKED
    | UNPACKED
    ;

// @400000-000100 [NIBBLES] SystemParams Zone
// {
//     SystemParams
// }
zone
    : AT range=NUMBER words NL* LBRACE blockBody RBRACE
    ;

// struct 000100 [NIBBLES] PatchParams
// {
//     byte Level
// }
structDef
    : STRUCT size=NUMBER? words NL* LBRACE blockBody RBRACE
    ;

// enum Switch
// {
//     Off
//     On
// }
enumDef
    : ENUM words NL* LBRACE enumBody RBRACE
    ;

blockBody
    : (NL | field)*
    ;

enumBody
    : (NL | enumValue)*
    ;

// byte Key[128]
// String Name[12]
// PatchParams[16 block]
// Tone[5 Part,4 Section]
// 0C Fine Tune
field
    : type=word nameWords=words? array?
    ;

array
    : LBRACK arrayDim (COMMA arrayDim)* COMMA? RBRACK
    ;

// "16 block" -> size=16, indexName=block
// "64"       -> size=64
arrayDim
    : size=NUMBER indexName=WORD?
    ;

// an enum value is a whole line (it may contain spaces)
enumValue
    : words
    ;

words
    : word+
    ;

word
    : WORD
    | NUMBER
    ;

// ============================================================
//  Lexer
// ============================================================

NL       : '\r'? '\n' ;
COMMENT  : '#' ~[\r\n]* -> skip ;
WS       : [ \t]+ -> skip ;
BOM      : '\uFEFF' -> skip ;

MODEL    : '@Model' ;
PACKED   : '@Packed' ;
UNPACKED : '@Unpacked' ;
AT       : '@' ;

STRUCT   : 'struct' ;
ENUM     : 'enum' ;

LBRACE   : '{' ;
RBRACE   : '}' ;
LBRACK   : '[' ;
RBRACK   : ']' ;
COMMA    : ',' ;

// "000100", "490A00", "400000-000100", "0C", "128"
NUMBER   : [0-9] [A-Za-z0-9_.\-]* ;

// "byte", "SystemParams", "Boss", "DS-330", "LFO_PITCH"
WORD     : [A-Za-z_] [A-Za-z0-9_.\-]* ;
