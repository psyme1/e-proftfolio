open SmallCTypes
open Utils
open TokenTypes

(* Parsing helpers (you don't need to modify these) *)

(* Return types for parse_stmt and parse_expr *)
type stmt_result = token list * stmt
type expr_result = token list * expr

(* Return the next token in the token list, throwing an error if the list is empty *)
let lookahead (toks : token list) : token =
  match toks with
  | [] -> raise (InvalidInputException "No more tokens")
  | h::_ -> h

(* Matches the next token in the list, throwing an error if it doesn't match the given token *)
let match_token (toks : token list) (tok : token) : token list =
  match toks with
  | [] -> raise (InvalidInputException(string_of_token tok))
  | h::t when h = tok -> t
  | h::_ -> raise (InvalidInputException(
      Printf.sprintf "Expected %s from input %s, got %s"
        (string_of_token tok)
        (string_of_list string_of_token toks)
        (string_of_token h)
    ))

(* Parsing (TODO: implement your code below) *)

let rec parse_expr toks : expr_result =
  let rec parse_primary tokens =
    match lookahead tokens with
      | Tok_Int n -> (Int n, match_token tokens (Tok_Int n))
      | Tok_Bool b -> (Bool b, match_token tokens (Tok_Bool b))
      | Tok_ID i -> (ID i, match_token tokens (Tok_ID i))
      | Tok_LParen -> 
        let t = match_token tokens Tok_LParen in
        let (t2, e) = parse_expr t in
        let t3 = match_token t2 Tok_RParen in
        (e, t3)
      |_ -> raise (InvalidInputException "Unclosed parenthesis")
  in
  let rec parse_unary tokens =
    match lookahead tokens with
    | Tok_Not ->
      let t1 = match_token tokens Tok_Not in
      let e2, t2 = parse_unary t1 in
      (Not (e2), t2)
    | _ -> parse_primary tokens
  in
  let rec parse_power tokens =
    let e1, t1 = parse_unary tokens in
    match lookahead t1 with
    | Tok_Pow ->
      let t2 = match_token t1 Tok_Pow in
      let e2, t3 = parse_power t2 in
      (Pow (e1, e2), t3)
    | _ -> (e1, t1)
    in

  let rec parse_multiplicative tokens =
    let e1, t1 = parse_power tokens in
    match lookahead t1 with
    | Tok_Mult ->
        let t2 = match_token t1 Tok_Mult in
        let e2, t3 = parse_multiplicative t2 in
        (Mult (e1, e2), t3)
    | Tok_Div ->
      let t2 = match_token t1 Tok_Div in
      let e2, t3 = parse_multiplicative t2 in
      (Div (e1, e2), t3)
    | _ -> (e1, t1)
  in

  let rec parse_additive tokens =
    let e1, t1 = parse_multiplicative tokens in
    match lookahead t1 with
    | Tok_Add ->
      let t2 = match_token t1 Tok_Add in
      let e2, t3 = parse_additive t2 in
      (Add (e1, e2), t3)
    | Tok_Sub ->
      let t2 = match_token t1 Tok_Sub in
      let e2, t3 = parse_additive t2 in
      (Sub (e1, e2), t3)
    | _ -> (e1,t1)
  in

  let rec parse_relational toks =
    let e1, t1 = parse_additive toks in
    let rec helper e1 t1 =
    match lookahead t1 with
    | Tok_Less ->
      let t2 = match_token t1 Tok_Less in
      let e2, t3 = parse_relational t2 in
      let aVal = Less (e1, e2) in
      helper aVal t3
    | Tok_Greater ->
      let t2 = match_token t1 Tok_Greater in
      let e2, t3 = parse_relational t2 in
      let aVal = Greater(e1, e2) in
      helper aVal t3
    | Tok_LessEqual ->
      let t2 = match_token t1 Tok_LessEqual in
      let e2, t3 = parse_relational t2 in
      let aVal = LessEqual(e1, e2) in
      helper aVal t3
    | Tok_GreaterEqual ->
      let t2 = match_token t1 Tok_GreaterEqual in
      let e2, t3 = parse_relational t2 in
      let aVal = GreaterEqual(e1, e2) in
      helper aVal t3
    | _ -> (e1, t1)
  in
  helper e1 t1
  in

  let rec parse_equality tokens = 
    let e1, t1 = parse_relational tokens in
    let rec helper e1 t1 =
      match lookahead t1 with
      | Tok_Equal ->
        let t2 = match_token t1 Tok_Equal in
        let e2, t3 = parse_relational t2 in
        helper (Equal (e1, e2)) t3
      | Tok_NotEqual ->
        let t2 = match_token t1 Tok_NotEqual in
        let e2, t3 = parse_relational t2 in
        helper (NotEqual (e1, e2)) t3
      | _ -> (e1, t1)
    in helper e1 t1
  in

  let rec parse_and tokens = 
    let e1, t1 = parse_equality tokens in
    let rec helper e1 t1 =
      match lookahead t1 with
      | Tok_And ->
        let t2 = match_token t1 Tok_And in
        let e2, t3 = parse_equality t2 in
        helper (And (e1, e2)) t3
      | _ -> (e1, t1)
    in helper e1 t1
  in

  let rec parse_or tokens = 
    let e1, t1 = parse_and tokens in
    let rec helper e1 t1 =
      match lookahead t1 with
      | Tok_Or ->
        let t2 = match_token t1 Tok_Or in
        let e2, t3 = parse_and t2 in
        helper (Or (e1, e2)) t3
      | _ -> (e1, t1)
    in helper e1 t1
  in
  let expr, tokens = parse_or toks in
  (tokens, expr)



let rec parse_stmt toks : stmt_result =
  match lookahead toks with
  | Tok_RBrace -> (toks, NoOp)
  | Tok_Int_Type | Tok_Bool_Type ->
    let data_type, t1 =
      match lookahead toks with
      | Tok_Int_Type -> (Int_Type, match_token toks Tok_Int_Type)
      | Tok_Bool_Type -> (Bool_Type, match_token toks Tok_Bool_Type)
      | _ -> raise (InvalidInputException "Invalid characters")
    in
    let id, t2 =
      match lookahead t1 with
      | (Tok_ID name) -> (name, match_token t1 (Tok_ID name))
      | _ -> raise (InvalidInputException "Invalid id")
    in
    let t3 = match_token t2 Tok_Semi in
    let t4, stmt_end = parse_stmt t3 in
    (t4, Seq (Declare (data_type, id), stmt_end))
  | Tok_ID name -> 
    let t1 = match_token toks (Tok_ID name) in
    let t2 = match_token t1 Tok_Assign in
    let t3, expr = parse_expr t2 in
    let t4 = match_token t3 Tok_Semi in
    let t5, stmt_end = parse_stmt t4 in
    (t5, Seq (Assign (name, expr), stmt_end))
  | Tok_Print ->
    let t1 = match_token toks Tok_Print in
    let t2 = match_token t1 Tok_LParen in
    let t3, expr = parse_expr t2 in
    let t4 = match_token t3 Tok_RParen in
    let t5 = match_token t4 Tok_Semi in
    let t6, stmt_end = parse_stmt t5 in
    (t6, Seq (Print expr, stmt_end))
  | Tok_If ->
    let t1 = match_token toks Tok_If in
    let t2 = match_token t1 Tok_LParen in
    let t3, expr = parse_expr t2 in
    let t4 = match_token t3 Tok_RParen in
    let t5 = match_token t4 Tok_LBrace in
    let t6, if_stmt = parse_stmt t5 in
    let t7 = match_token t6 Tok_RBrace in
    (match lookahead t7 with
      | Tok_Else ->
        let t8 = match_token t7 Tok_Else in
        let t9 = match_token t8 Tok_LBrace in
        let t10, else_stmt = parse_stmt t9 in
        let t11 = match_token t10 Tok_RBrace in
        let t12, rest = parse_stmt t11 in
        (t12, Seq(If (expr, if_stmt, else_stmt), rest))
      | _ -> 
        let t8, rest = parse_stmt t7 in
        (t8, Seq(If (expr, if_stmt, NoOp), rest)))
  | Tok_For ->
    let t1 = match_token toks Tok_For in
    let t2 = match_token t1 Tok_LParen in
    let name, t3 =
      match lookahead t2 with
      | (Tok_ID id) -> (id, match_token t2 (Tok_ID id))
      | _ -> raise (InvalidInputException "ID not properly setup")
    in
    let t4 = match_token t3 Tok_From in
    let t5, expr1 = parse_expr t4 in
    let t6 = match_token t5 Tok_To in
    let t7, expr2 = parse_expr t6 in
    let t8 = match_token t7 Tok_RParen in
    let t9 = match_token t8 Tok_LBrace in
    let t10, for_stmt = parse_stmt t9 in
    let t11 = match_token t10 Tok_RBrace in
    let t12, rest = parse_stmt t11 in
    (t12, Seq(For (name, expr1, expr2, for_stmt), rest))
  | Tok_While ->
    let t1 = match_token toks Tok_While in
    let t2 = match_token t1 Tok_LParen in
    let t3, expr = parse_expr t2 in
    let t4 = match_token t3 Tok_RParen in
    let t5 = match_token t4 Tok_LBrace in
    let t6, while_stmt = parse_stmt t5 in
    let t7 = match_token t6 Tok_RBrace in
    let t8, rest = parse_stmt t7 in
    (t8, Seq (While (expr, while_stmt), rest))
  | _ -> (toks, NoOp)

let parse_main toks : stmt =
  let t0 = match_token toks Tok_Int_Type in (*I forgot the int keyword*)
  let t1 = match_token t0 Tok_Main in
  let tp = match_token t1 Tok_LParen in (*and the braces tooo*)
  let tp2 = match_token tp Tok_RParen in
  let t2 = match_token tp2 Tok_LBrace in
  let t3, main_stmt = parse_stmt t2 in
  let t4 = match_token t3 Tok_RBrace in
  match lookahead t4 with
    | EOF -> main_stmt
    | _ -> (raise (InvalidInputException "Expected EOF at the end of program"))
