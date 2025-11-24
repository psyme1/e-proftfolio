open Types
open Utils

(* Provided functions - DO NOT MODIFY *)

(* Matches the next token in the list, throwing an error if it doesn't match the given token *)
let match_token (toks : token list) (tok : token) =
  match toks with
  | [] -> raise (InvalidInputException (string_of_token tok))
  | h :: t when h = tok -> t
  | h :: _ ->
      raise
        (InvalidInputException
           (Printf.sprintf "Expected %s from input %s, got %s"
              (string_of_token tok)
              (string_of_list string_of_token toks)
              (string_of_token h)))

(* Matches a sequence of tokens given as the second list in the order in which they appear, throwing an error if they don't match *)
let match_many (toks : token list) (to_match : token list) =
  List.fold_left match_token toks to_match

(* Return the next token in the token list as an option *)
let lookahead (toks : token list) =
  match toks with [] -> None | h :: t -> Some h

(* Return the token at the nth index in the token list as an option*)
let rec lookahead_many (toks : token list) (n : int) =
  match (toks, n) with
  | h :: _, 0 -> Some h
  | _ :: t, n when n > 0 -> lookahead_many t (n - 1)
  | _ -> None

(* Part 2: Parsing expressions *)

let rec parse_expr toks = 
  let rec parse_record_expr toks =
    let toks = match_token toks Tok_LCurly in
    let rec ea toks acc =
      match lookahead toks with
      | Some Tok_RCurly ->
        let toks = match_token toks Tok_RCurly in
        (toks, Record (List.rev acc))
      | Some (Tok_ID id) ->
        let toks = match_token toks (Tok_ID id) in
        let toks = match_token toks Tok_Equal in
        let (toks, value) = parse_expr toks in
        let (toks, acc) =
        match lookahead toks with
        | Some Tok_Semi ->
          let toks = match_token toks Tok_Semi in
          (toks, (Lab id, value) :: acc)
        | _ ->
          (toks, (Lab id, value) :: acc)
      in
      ea toks acc
    | _ -> raise (InvalidInputException "Couldn't find that")
    in
    ea toks []
  in
  let rec parse_primary toks =
    match lookahead toks with
    | Some (Tok_Int n) ->
      (List.tl toks, Int n)
    | Some (Tok_Bool b) ->
      (List.tl toks, Bool b)
    | Some (Tok_String s) ->
      (List.tl toks, String s)
    | Some (Tok_ID i) ->
      (List.tl toks, ID i)
    | Some (Tok_LParen) ->
      let toks = match_token toks Tok_LParen in
      let (toks, e) = parse_expr toks in
      let toks = match_token toks Tok_RParen in
      (toks, e)
    | Some (Tok_LCurly) ->
      parse_record_expr toks
    | Some (Tok_Def) ->
      raise (InvalidInputException "Def not accepted")
    in
    let rec parse_dot_expr toks =
      let (toks, e) = parse_primary toks in
      match lookahead toks with
      | Some Tok_Dot ->
        let toks = match_token toks Tok_Dot in
        (match lookahead toks with
        | Some (Tok_ID id) ->
          let toks = List.tl toks in
          (toks, Select (Lab id, e))
        | _ -> raise  (InvalidInputException "Should have an ID after dot"))
      | _ -> (toks, e)
    in
    let rec parse_apply_expr toks =
      let (toks1, func) = parse_dot_expr toks in
      match lookahead toks1 with
      | Some (Tok_Int _ | Tok_Bool _ | Tok_String _ | Tok_ID _ | Tok_LParen | Tok_LCurly) ->
        let (toks2, arg) = parse_apply_expr toks1 in
        (toks2, App (func, arg))
      | _ -> (toks1, func)
    in
    let rec parse_unary_expr toks =
      match lookahead toks with
      | Some Tok_Not ->
        let toks = match_token toks Tok_Not in
        let (toks, e) = parse_unary_expr toks in
        (toks, Not e)
      | Some Tok_Sub ->
        (match lookahead_many toks 1 with
        | Some (Tok_Int _ | Tok_ID _ | Tok_LParen | Tok_LCurly) ->
          let toks = match_token toks Tok_Sub in
          let (toks, e) = parse_unary_expr toks in
          (toks, Binop (Sub, Int 0, e))
        | _ -> parse_apply_expr toks)
      | _ -> parse_apply_expr toks
    in
    let rec parse_concat_expr toks =
      let (toks1, lstr) = parse_unary_expr toks in
      match lookahead toks1 with
      | Some Tok_Concat ->
        let toks2 = match_token toks1 Tok_Concat in
        let (toks3, rstr) = parse_concat_expr toks2 in
        (toks3, Binop (Concat, lstr, rstr))
      | _ -> (toks1, lstr)
    in
    let rec parse_multiplicative_expr toks =
      let (toks1, lnum) = parse_concat_expr toks in
      match lookahead toks1 with
      | Some Tok_Mult ->
        let toks2 = match_token toks1 Tok_Mult in
        let (toks3, rnum) = parse_multiplicative_expr toks2 in
        (toks3, Binop (Mult, lnum, rnum))
      | Some Tok_Div ->
        let toks2 = match_token toks1 Tok_Div in
        let (toks3, rnum) = parse_multiplicative_expr toks2 in
        (toks3, Binop (Div, lnum, rnum))
      | _ -> (toks1, lnum)
    in
    let rec parse_additive_expr toks =
      let (toks, first) = parse_multiplicative_expr toks in
      let rec addLoop left toks =
        match lookahead toks with
        | Some Tok_Add ->
          let toks = match_token toks Tok_Add in
          let (toks, rnum) = parse_multiplicative_expr toks in
          addLoop (Binop(Add, left, rnum)) toks
        | Some Tok_Sub ->
          let toks = match_token toks Tok_Sub in
          let (toks, rnum) = parse_multiplicative_expr toks in
          addLoop (Binop(Sub, left, rnum)) toks
        | _ -> (toks, left)
      in
      addLoop first toks
    in
    let rec parse_relational_expr toks =
      let (toks1, lb) = parse_additive_expr toks in
      match lookahead toks1 with
      | Some Tok_Less ->
        let toks2 = match_token toks1 Tok_Less in
        let toks3, (rb) = parse_relational_expr toks2 in
        (toks3, Binop (Less, lb, rb))
      | Some Tok_LessEqual ->
        let toks2 = match_token toks1 Tok_LessEqual in
        let (toks3, rb) = parse_relational_expr toks2 in
        (toks3, Binop (LessEqual, lb, rb))
      | Some Tok_Greater ->
        let toks2 = match_token toks1 Tok_Greater in
        let (toks3, rb) = parse_relational_expr toks2 in
        (toks3, Binop (Greater, lb, rb))
      | Some Tok_GreaterEqual ->
        let toks2 = match_token toks1 Tok_GreaterEqual in
        let (toks3, rb) = parse_relational_expr toks2 in
        (toks3, Binop (GreaterEqual, lb, rb))
      | _ -> (toks1, lb)
    in
    let rec parse_equality_expr toks =
      let (toks1, left) = parse_relational_expr toks in
      match lookahead toks1 with
      | Some Tok_Equal ->
        let toks2 = match_token toks1 Tok_Equal in
        let (toks3, right) = parse_equality_expr toks2 in
        (toks3, Binop (Equal, left, right))
      | Some Tok_NotEqual ->
        let toks2 = match_token toks1 Tok_NotEqual in
        let (toks3, right) = parse_equality_expr toks2 in
        (toks3, Binop (NotEqual, left, right))
      | _ -> (toks1, left)
    in
    let rec parse_and_expr toks =
      let (toks1, left) = parse_equality_expr toks in
      match lookahead toks1 with
      | Some Tok_And ->
        let toks2 = match_token toks1 Tok_And in
        let (toks3, right) = parse_and_expr toks2 in
        (toks3, Binop (And, left, right))
      | _ -> (toks1, left)
    in
    let rec parse_or_expr toks =
      let (toks1, left) = parse_and_expr toks in
      match lookahead toks1 with
      | Some Tok_Or ->
        let toks2 = match_token toks1 Tok_Or in
        let (toks3, right) = parse_and_expr toks2 in
        (toks3, Binop (Or, left, right))
      | _ -> (toks1, left)
    in
    let rec parse_if_expr toks =
      let toks = match_token toks Tok_If in
      let (toks, cond) = parse_expr toks in
      let toks = match_token toks Tok_Then in
      let (toks, t_branch) = parse_expr toks in
      let toks = match_token toks Tok_Else in
      let (toks, e_branch) = parse_expr toks in
      (toks, If (cond, t_branch, e_branch))
    in
    let rec parse_let_expr toks =
      let toks = match_token toks Tok_Let in
      let (toks, is_rec) = 
        match lookahead toks with
        | Some Tok_Rec -> (match_token toks Tok_Rec, true)
        | _ -> (toks, false)
      in
      match lookahead toks with
      | Some (Tok_ID name) ->
        let toks = match_token toks (Tok_ID name) in
        let toks = match_token toks Tok_Equal in
        let (toks, e) = parse_expr toks in
        let toks = match_token toks Tok_In in
        let (toks, body) = parse_expr toks in
        (toks, Let (name, is_rec, e, body))
      | _ -> raise (InvalidInputException "no identifier found")
    in
    let rec parse_fun_expr toks =
      let toks = match_token toks Tok_Fun in
      match lookahead toks with
      | Some (Tok_ID name) ->
        let toks = match_token toks (Tok_ID name) in
        let toks = match_token toks Tok_Arrow in
        let (toks, body) = parse_expr toks in
        (toks, Fun (name,body))
      | _ -> raise (InvalidInputException "no identifier found")
    in
    match lookahead toks with
      | Some Tok_Let -> parse_let_expr toks
      | Some Tok_If -> parse_if_expr toks
      | Some Tok_Fun -> parse_fun_expr toks
      | Some _ -> parse_or_expr toks
      | None -> raise (InvalidInputException "Invalid")

(* Part 3: Parsing mutop *)

let rec parse_mutop toks =
  match lookahead toks with
  | Some Tok_Def ->
    let toks = match_token toks Tok_Def in
    (match lookahead toks with
    | Some (Tok_ID name) -> 
        let toks = match_token toks (Tok_ID name) in
        let toks = match_token toks Tok_Equal in
        let (toks, e) = parse_expr toks in
        let toks = match_token toks Tok_DoubleSemi in
        ([], Def (name, e))
        | _ -> raise (InvalidInputException "need identifier"))
  | Some Tok_DoubleSemi ->
    let toks = match_token toks Tok_DoubleSemi in
    ([], NoOp)
  | Some _ ->
    let (toks, e) = parse_expr toks in
    let toks = match_token toks Tok_DoubleSemi in
    ([], Expr e)
  | None -> raise (InvalidInputException "no token")