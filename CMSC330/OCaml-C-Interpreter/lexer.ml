open TokenTypes


let tokenize input =
  let rec tokenize input =
    let len = String.length input in
    let re_ws = Re.compile (Re.Perl.re "^[\\s\\t\\n]+") in
    let re_for = Re.compile (Re.Perl.re "^for") in
    let re_from = Re.compile (Re.Perl.re "^from") in
    let re_to = Re.compile (Re.Perl.re "^to") in
    let re_while = Re.compile (Re.Perl.re "^while") in
    let re_int_type = Re.compile (Re.Perl.re "^int") in
    let re_bool_type = Re.compile (Re.Perl.re "^bool") in
    let re_sub = Re.compile (Re.Perl.re "^-") in
    let re_semi = Re.compile (Re.Perl.re "^;") in
    let re_lparen = Re.compile (Re.Perl.re "^\\(") in
    let re_rparen = Re.compile (Re.Perl.re "^\\)") in
    let re_lbrace = Re.compile (Re.Perl.re "^\\{") in
    let re_rbrace = Re.compile (Re.Perl.re "^\\}") in
    let re_printf = Re.compile (Re.Perl.re "^printf") in
    let re_pow = Re.compile (Re.Perl.re "^\\^") in
    let re_add = Re.compile (Re.Perl.re "^\\+") in
    let re_or = Re.compile (Re.Perl.re "^\\|\\|") in
    let re_notequal = Re.compile (Re.Perl.re "^!=") in
    let re_not = Re.compile (Re.Perl.re "^!") in
    let re_mult = Re.compile (Re.Perl.re "^\\*") in
    let re_main = Re.compile (Re.Perl.re "^main") in
    let re_lessequal = Re.compile (Re.Perl.re "^<=") in
    let re_less = Re.compile (Re.Perl.re "^<") in
    let re_int = Re.compile (Re.Perl.re "^(-?[0-9]+)") in
    let re_if = Re.compile (Re.Perl.re "^if") in
    let re_id = Re.compile (Re.Perl.re "^[a-zA-Z][a-zA-Z0-9]*") in
    let re_greaterequal = Re.compile (Re.Perl.re "^>=") in
    let re_greater = Re.compile (Re.Perl.re "^>") in
    let re_equal = Re.compile (Re.Perl.re "^==") in
    let re_else = Re.compile (Re.Perl.re "^else") in
    let re_div = Re.compile (Re.Perl.re "^/") in
    let re_bool = Re.compile (Re.Perl.re "^(true|false)") in
    let re_assign = Re.compile (Re.Perl.re "^=") in
    let re_and = Re.compile (Re.Perl.re "^\\&\\&") in

    if input = "" then [EOF]
    else if Re.execp re_ws input then 
      let wsgroup = Re.exec re_ws input in 
      let wslen = String.length (Re.Group.get wsgroup 0) in
      (tokenize (String.sub input wslen (len - wslen)))
    else if Re.execp re_for input then
      Tok_For::(tokenize (String.sub input 3 (len - 3)))
    else if Re.execp re_from input then
      Tok_From::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_to input then
      Tok_To::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_while input then
      Tok_While::(tokenize (String.sub input 5 (len - 5)))
    else if Re.execp re_int_type input then (
      Tok_Int_Type::(tokenize (String.sub input 3 (len - 3)))
    )
    else if Re.execp re_bool_type input then
      Tok_Bool_Type::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_sub input then
      Tok_Sub::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_semi input then
      Tok_Semi::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_lparen input then
      Tok_LParen::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_rparen input then
      Tok_RParen::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_lbrace input then
      Tok_LBrace::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_rbrace input then
      Tok_RBrace::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_printf input then
      Tok_Print::(tokenize (String.sub input 6 (len - 6)))
    else if Re.execp re_pow input then
      Tok_Pow::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_add input then
        Tok_Add::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_or input then
        Tok_Or::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_notequal input then
        Tok_NotEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_not input then
        Tok_Not::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_mult input then
        Tok_Mult::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_main input then
        Tok_Main::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_lessequal input then
        Tok_LessEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_less input then
        Tok_Less::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_int input then
      let numgroup = Re.exec re_int input in
      let num = Re.Group.get numgroup 1 in
      let numlen = String.length num in
      let numint = int_of_string num in
      Tok_Int(numint)::(tokenize (String.sub input numlen (len - numlen)))
    else if Re.execp re_if input then
      Tok_If::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_greaterequal input then
      Tok_GreaterEqual::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_greater input then
        Tok_Greater::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_equal input then
        Tok_Equal::(tokenize (String.sub input 2 (len - 2)))
    else if Re.execp re_else input then
        Tok_Else::(tokenize (String.sub input 4 (len - 4)))
    else if Re.execp re_div input then
        Tok_Div::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_bool input then
        let boolgroup = Re.exec re_bool input in
        let boolstr = Re.Group.get boolgroup 0 in
        let boollen = String.length boolstr in
        let boolval = (boolstr = "true") in
        Tok_Bool(boolval)::(tokenize (String.sub input boollen (len - boollen)))
    else if Re.execp re_id input then (
      let idgroup = Re.exec re_id input in
      let id = Re.Group.get idgroup 0 in
      let idlen = String.length id in
      Tok_ID(id)::(tokenize (String.sub input idlen (len - idlen)))
    )
    else if Re.execp re_assign input then
        Tok_Assign::(tokenize (String.sub input 1 (len - 1)))
    else if Re.execp re_and input then
        Tok_And::(tokenize (String.sub input 2 (len - 2)))
    else(
      failwith "tokenization error"
      )
  in
  tokenize input
  ;;