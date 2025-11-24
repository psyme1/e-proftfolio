open Types

(* Provided functions - DO NOT MODIFY *)

(* Adds mapping [x:v] to environment [env] *)
let extend env x v = (x, ref v) :: env

(* Returns [v] if [x:v] is a mapping in [env]; uses the
   most recent if multiple mappings for [x] are present *)
let rec lookup env x =
  match env with
  | [] -> raise (DeclareError ("Unbound variable " ^ x))
  | (var, value) :: t -> if x = var then !value else lookup t x

(* Creates a placeholder mapping for [x] in [env]; needed
   for handling recursive definitions *)
let extend_tmp env x = (x, ref (Int 0)) :: env

(* Updates the (most recent) mapping in [env] for [x] to [v] *)
let rec update env x v =
  match env with
  | [] -> raise (DeclareError ("Unbound variable " ^ x))
  | (var, value) :: t -> if x = var then value := v else update t x v

(* Part 1: Evaluating expressions *)

(* Evaluates MicroCaml expression [e] in environment [env],
   returning an expression, or throwing an exception on error *)
let rec eval_expr env e =
  match e with
  | Int n -> Int n
  | Bool b -> Bool b
  | String s -> String s
  | ID x -> lookup env x
  | Not e1 -> 
    let v = eval_expr env e1 in
    (match v with
    | Bool b -> Bool (not b)
    | _ -> raise (TypeError "Not requires bool"))
  | Binop (op, e1, e2) ->
    let v1 = eval_expr env e1 in
    let v2 = eval_expr env e2 in
    (match op, v1, v2 with
    | Add, Int n1, Int n2 -> Int (n1 + n2)
    | Sub, Int n1, Int n2 -> Int (n1 - n2)
    | Mult, Int n1, Int n2 -> Int (n1 * n2)
    | Div, Int n1, Int 0 -> raise (DivByZeroError)
    | Div, Int n1, Int n2 -> Int (n1 / n2)
    | Equal, v1, v2 -> Bool (v1 = v2)
    | NotEqual, v1, v2 -> Bool (v1 <> v2)
    | Greater, Int n1, Int n2 -> Bool (n1 > n2)
    | Less, Int n1, Int n2 -> Bool (n1 < n2)
    | GreaterEqual, Int n1, Int n2 -> Bool (n1 >= n2)
    | LessEqual, Int n1, Int n2 -> Bool (n1 <= n2)
    | And, Bool b1, Bool b2 -> Bool (b1 && b2)
    | Or, Bool b1, Bool b2 -> Bool (b1 || b2)
    | Concat, String s1, String s2 -> String (s1 ^ s2)
    | _ -> raise (TypeError "Wrong types for operation"))
  | If (cond, t_branch, e_branch) ->
    let cond = eval_expr env cond in
    (match cond with
    | Bool true -> eval_expr env t_branch
    | Bool false -> eval_expr env e_branch
    | _ -> raise (TypeError "Condition must be boolean"))
  | Let (name, isRec, e1, e2) ->
    if isRec then
      let recEnv = extend_tmp env name in
      let v1 = eval_expr recEnv e1 in
      let _ = update recEnv name v1 in
      eval_expr recEnv e2
    else
      let v1 = eval_expr env e1 in
      let newEnv = extend env name v1 in
      eval_expr newEnv e2
  | Fun (arg, body) -> Closure (env, arg, body)
  | App (f, arg) ->
    let v = eval_expr env f in
    let val2 = eval_expr env arg in
    (match v with
    | Closure (env, param, body) ->
      let newEnv = extend env param val2 in
      eval_expr newEnv body
    | _ -> raise (TypeError "Tried to apply a non-function"))
  | Record fields ->
    let eval (lab, expr) = (lab, eval_expr env expr) in
    Record (List.map eval fields)
  | Select (lab, expr)->
    match eval_expr env expr with
    | Record fields ->
      (match List.find_opt (fun (l, _) -> l = lab) fields with
      | Some (_, value) -> value
      | None -> raise (DeclareError "Field not in it"))
    | _ -> raise (TypeError "Expected a recorded")

(* Part 2: Evaluating mutop directive *)

(* Evaluates MicroCaml mutop directive [m] in environment [env],
   returning a possibly updated environment paired with
   a value option; throws an exception on error *)
let eval_mutop env m = 
  match m with
  | NoOp ->
    (env, None)
  | Expr e ->
    let v = eval_expr env e in
    (env, Some v)
  | Def (name, e) ->
    let v = eval_expr env e in
    let newEnv = extend env name v in
    (newEnv, Some v)
  