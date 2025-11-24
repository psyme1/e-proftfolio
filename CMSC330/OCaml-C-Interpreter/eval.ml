open SmallCTypes
open Utils
open TokenTypes

exception TypeError of string
exception DeclareError of string
exception DivByZeroError

let rec eval_expr env t =
  match t with
    | ID name ->
      (match List.assoc_opt name env with 
        | Some v -> v
        | _ -> raise (DeclareError "Value not defined"))
    | Int n -> Int_Val n
    | Bool b -> Bool_Val b
    | Add(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match (n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Int_Val (n3 + n4)
      | _ -> raise (TypeError "Add only adds two ints"))
    | Sub(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match (n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Int_Val (n3 - n4)
      | _ -> raise (TypeError "Subtraction subtracts two ints"))
    | Mult(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match (n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Int_Val (n3 * n4)
      | _ -> raise (TypeError "Multiplcation requires two integers"))
    | Div(e1, e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match (n1,n2) with
      | (Int_Val n3, Int_Val n4) ->
          if n4 = 0 then raise DivByZeroError
          else Int_Val (n3 / n4)
      | _ -> raise (TypeError "Division requires two integers"))
    | Pow(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Int_Val (int_of_float (float_of_int n3 ** float_of_int n4))
      | _ -> raise (TypeError "Power requires two integers"))
    | Greater(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (n3 > n4)
      | _ -> raise (TypeError "greater than requires two integers"))
    | Less(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (n3 < n4)
      | _ -> raise (TypeError "Less than needs two integers"))
    | GreaterEqual(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (n3 >= n4)
      | _ -> raise (TypeError "Greater Equal than needs two integers"))
    | LessEqual(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (n3 <= n4)
      | _ -> raise (TypeError "Less Equal requires two integers"))
    | Equal(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (n3 = n4)
      | (Bool_Val b1, Bool_Val b2) -> Bool_Val (b1 = b2)
      | _ -> raise (TypeError "Equal needs to comprae same type"))
    | NotEqual(e1,e2) ->
      let n1 = eval_expr env e1 in
      let n2 = eval_expr env e2 in
      (match(n1,n2) with
      | (Int_Val n3, Int_Val n4) -> Bool_Val (not (n3 = n4))
      | (Bool_Val b1, Bool_Val b2) -> Bool_Val (not (b1 = b2))
      | _ -> raise (TypeError "Equal needs to comprae same type"))
    | Or(e1,e2) ->
      let b1 = eval_expr env e1 in
      let b2 = eval_expr env e2 in
      (match(b1,b2) with
      | (Bool_Val b3, Bool_Val b4) -> Bool_Val (b3 || b4)
      | _ -> raise (TypeError "Or should compare two boolean expressions"))
    | And(e1,e2) ->
      let b1 = eval_expr env e1 in
      let b2 = eval_expr env e2 in
      (match(b1,b2) with
      | (Bool_Val b3, Bool_Val b4) -> Bool_Val (b3 && b4)
      | _ -> raise (TypeError "And should compare two boolean expressions"))
    | Not(e1) ->
      let b1 = eval_expr env e1 in
      (match(b1) with
      | (Bool_Val b2) -> Bool_Val (not b2)
      | _ -> raise (TypeError "Not should be applied on a boolean"))

let rec eval_stmt env s =
  match s with
  | NoOp -> env
  | Seq(stmt1, stmt2) ->
    let env2 = eval_stmt env stmt1 in
    eval_stmt env2 stmt2
  | Declare(t, name) ->
    (match List.assoc_opt name env with
    | Some _ -> raise (DeclareError "You can't redeclare same variable")
    | None ->
      let v = match t with
      | Int_Type -> Int_Val 0
      | Bool_Type -> Bool_Val false
      in
      (name, v) :: env)
  | Assign(name, expr) ->
    (match List.assoc_opt name env with
    | None -> raise (DeclareError "Can't assign undeclared variable")
    | Some old -> 
      let newVal = eval_expr env expr in
      let valid = match (old, newVal) with
      | (Int_Val _, Int_Val _) -> true
      | (Bool_Val _, Bool_Val _) -> true
      | _ -> false
      in
      if not valid then raise (TypeError "Can't assign different types")
      else
        let env_without_old = List.filter (fun (k, _) -> k <> name) env in
        (name, newVal) :: env_without_old)
  | If(b, i, e) ->
    (match eval_expr env b with
    | Bool_Val true -> eval_stmt env i
    | Bool_Val false -> eval_stmt env e
    | _ -> raise (TypeError "If statement should contain a boolean expression"))
  | While(expr, body) ->
    let rec loop env = 
      match eval_expr env expr with
      | Bool_Val true -> loop (eval_stmt env body)
      | Bool_Val false -> env
      | _ -> raise (TypeError "While loop should contain a boolean expression")
    in
    loop env
  | For(name, expr1, expr2, stmt) ->
    (match (eval_expr env expr1, eval_expr env expr2) with
    | (Int_Val start, Int_Val stop) ->  (*why did end have to a reserved keyword*)
      let rec loop env counter =
        if counter > stop then env
        else
          let updateEnv = (name, Int_Val counter) :: (List.remove_assoc name env) in
          let nw = eval_stmt updateEnv stmt in  (*Everything is a reserved keyword :c *)
          let inc =
            match List.assoc_opt name nw with
            | Some (Int_Val v) -> v + 1
            | _ -> counter + 1  (*self incrementing*)
          in
          loop ((name, Int_Val inc) :: (List.remove_assoc name nw)) inc  (*update the value at end of loop*)
      in
      loop env start
    | _ -> raise (TypeError "For loop not properly setup"))
  | Print(expr) ->
    let v = eval_expr env expr in  (*read what to print*)
    let _ =
      match v with
      | Int_Val i ->
          let _ = print_output_int i in
          print_output_newline ()  (*newline after printing*)
      | Bool_Val b ->
          let _ = print_output_bool b in
          print_output_newline ()
    in
    env  (*return the env*)