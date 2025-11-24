open Types

(* Part 1: Lexer - IMPLEMENT YOUR CODE BELOW *)

let tokenize input = 
  let rec tokenize input=
    let len = String.length input in
    let re_ws = Re.compile (Re.Perl.re "^[\\t\\s\\n]+") in
    let re_rparen = Re.compile (Re.Perl.re "^\\)") in
    let re_lpraen = Re.compile (Re.Perl.re "^\\(") in
    let re_rcurly = Re.compile (Re.Perl.re "^\\}") in
    let re_lcurly = Re.compile (Re.Perl.re "^\\{") in
    let re_dot = Re.compile (Re.Perl.re "^[.]") in
    let re_equal = Re.compile (Re.Perl.re "^=") in
    let re_notequal = Re.compile (Re.Perl.re "^<>") in
    let re_greater = Re.compile (Re.Perl.re "^>") in
    let re_less = Re.compile (Re.Perl.re "^<") in
    let re_let = Re.compile (Re.Perl.re "^let") in
    let re_greaterequal = Re.compile (Re.Perl.re "^>=") in
    let re_lessequal = Re.compile (Re.Perl.re "^<=") in
    let re_or = Re.compile (Re.Perl.re "^\\|\\|") in
    let re_and = Re.compile (Re.Perl.re "^&&") in
    let re_not = Re.compile (Re.Perl.re "^not") in
    let re_mult = Re.compile (Re.Perl.re "^\\*") in
    let re_add = Re.compile (Re.Perl.re "^\\+") in
    let re_sub = Re.compile (Re.Perl.re "^-") in
    let re_int = Re.compile (Re.Perl.re "^(-?[0-9]+)") in
    let re_if = Re.compile (Re.Perl.re "^if") in
    let re_fun = Re.compile (Re.Perl.re "^fun") in
    let re_then = Re.compile (Re.Perl.re "^then") in
    let re_else = Re.compile (Re.Perl.re "^else") in
    let re_div = Re.compile (Re.Perl.re "^/") in
    let re_bool = Re.compile (Re.Perl.re "^(true|false)") in
    let re_concat = Re.compile (Re.Perl.re "^\\^") in
    let re_def = Re.compile (Re.Perl.re "^def") in
    let re_in = Re.compile (Re.Perl.re "^in") in
    let re_rec = Re.compile (Re.Perl.re "^rec") in
    let re_arrow = Re.compile (Re.Perl.re "^->") in
    let re_dumblesemi = Re.compile (Re.Perl.re "^;;") in
    let re_semi = Re.compile (Re.Perl.re "^;") in
    let re_id = Re.compile (Re.Perl.re "^([a-zA-Z][a-zA-Z0-9]*)") in
    let re_string = Re.compile (Re.Perl.re "^\"([^\"]*)\"") in

    if input = "" then []
    else if Re.execp re_ws input then
      let group = Re.exec re_ws input in 
      let len2 = String.length (Re.Group.get group 0) in
      tokenize (String.sub input len2 (len - len2))
    else if Re.execp re_rparen input then
      Tok_RParen::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_lpraen input then
      Tok_LParen::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_rcurly input then
      Tok_RCurly::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_lcurly input then
      Tok_LCurly::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_arrow input then
      Tok_Arrow::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_dot input then
      Tok_Dot::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_equal input then
      Tok_Equal::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_notequal input then
      Tok_NotEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_greater input then
      Tok_Greater::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_less input then
      Tok_Less::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_greaterequal input then
      Tok_GreaterEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_lessequal input then
      Tok_LessEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_let input then
      Tok_Let::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_or input then
      Tok_Or::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_and input then
      Tok_And::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_not input then
      Tok_Not::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_mult input then
      Tok_Mult::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_add input then
      Tok_Add::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_sub input then
      Tok_Sub::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_int input then
      let group = Re.exec re_int input in
      let num = Re.Group.get group 1 in
      let len2 = String.length num in
      let parse = int_of_string num in
      Tok_Int(parse)::(tokenize (String.sub input len2 (len - len2)))
    else if Re.execp re_if input then
      Tok_If::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_then input then
      Tok_Then::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_else input then
      Tok_Else::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_div input then
      Tok_Div::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_bool input then
      let group = Re.exec re_bool input in
      let b = Re.Group.get group 0 in
      let o = (b = "true") in
      let len2 = String.length b in
      Tok_Bool(o)::(tokenize (String.sub input len2 (len - len2)))
    else if Re.execp re_concat input then
      Tok_Concat::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_def input then
      Tok_Def::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_in input then
      Tok_In::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_rec input then
      Tok_Rec::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_fun input then
      Tok_Fun::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_dumblesemi input then
      Tok_DoubleSemi::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_semi input then
      Tok_Semi::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_string input then
      let group = Re.exec re_string input in
      let full = Re.Group.get group 0 in
      let str = Re.Group.get group 1 in
      let len2 = String.length full in
      Tok_String(str)::(tokenize (String.sub input len2 (len - len2)))
    else if Re.execp re_id input then
      let group = Re.exec re_id input in
      let id = Re.Group.get group 1 in
      let len2 = String.length id in
      Tok_ID(id)::(tokenize (String.sub input len2 (len - len2)))
    else
      failwith ("Tokenization error")
    in
    tokenize input