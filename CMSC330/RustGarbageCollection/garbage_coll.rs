use std::collections::{HashMap, HashSet, VecDeque};
use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;
use regex::Regex;

use crate::types::{Memory, RefCountMem};
use crate::utils::*;

fn remove(heap: &mut Vec<(Option<Vec<u32>>, u32)>, index: u32){
    let idx =  index as usize;
    if heap[idx].1 == 0 {
        return;
    }
    heap[idx].1 -= 1;
    if heap[idx].1 == 0 {
        if let Some(children) = heap[idx].0.take() {
            for child in children {
                remove(heap, child);
            }
        }
    }
}

pub fn reference_counting(string_vec: Vec<String>) ->  RefCountMem {
    let mut stack = Vec::new();
    let mut heap = vec![(None, 0); 10];
    let re_heap = Regex::new(r"Ref Heap (\d+)(?: (.*))?").unwrap();
    let re_stack = Regex::new(r"Ref Stack(?: (.*))?").unwrap();
    let re_pop =  Regex::new(r"Pop").unwrap();

    for line in string_vec{
        if re_pop.is_match(&line){
            if let Some(frame) = stack.pop() {
                for idx in frame {
                    remove(&mut heap, idx);
                }
            }
        }
        else if let Some(caps) = re_heap.captures(&line) {
            let idx: usize = caps[1].parse().unwrap();
            let children: Vec<u32> = caps.get(2).map(|x| x.as_str().split_whitespace().filter_map(|x| x.parse().ok()).collect()).unwrap_or_default();
            let slot = &mut heap[idx];
            if slot.1 > 0 || slot.0.is_some() {
                if let Some(val) = slot.0.take() {
                    for child in val{
                        remove(&mut heap, child)
                    }
                }
                heap[idx].0 = Some(children.clone());
                for &child in &children{
                    let child_slot = &mut heap[child as usize];
                    if child_slot.0.is_none() {
                        child_slot.0 = Some(Vec::new())
                    }
                    child_slot.1 += 1;
                }
            }

        }else if let Some(caps) = re_stack.captures(&line) {
            let frame: Vec<u32> = caps.get(1).map(|x| x.as_str().split_whitespace().filter_map(|x| x.parse().ok()).collect()).unwrap_or_default();
            for &idx in &frame {
                let slot =  &mut heap[idx as usize];
                if slot.0.is_none(){
                    slot.0 = Some(Vec::new());
                }
                slot.1 += 1;
            }
            stack.push(frame);
        }
    }
    for entry in  heap.iter_mut(){
        if entry.1 == 0 {
            entry.0 =  None;
        }
    }

    RefCountMem { stack, heap }
}

// suggested helper function. You may modify parameters as you wish.
// Takes in some form of stack and heap and returns all indicies in heap
// that can be reached.
pub fn reachable(stack: &Vec<Vec<u32>>, heap: &Vec<Option<(String, Vec<u32>)>>) -> Vec<u32> {
    let mut visited = HashSet::new();
    let mut queue = VecDeque::new();

    for x in stack{
        for &i in x{
            if visited.insert(i){
                queue.push_back(i);
            }
        }
    }
    while let Some(x) = queue.pop_front(){
        if let Some(Some((_, reference))) = heap.get(x as usize)  {
            for &i in reference{
                if visited.insert(i){
                    queue.push_back(i);
                }
            }
        }
    }

    visited.into_iter().collect()
} 

pub fn mark_and_sweep(mem: &mut Memory) -> () {
    let re = reachable(&mem.stack, &mem.heap);
    for(i, value) in mem.heap.iter_mut().enumerate(){
        if !re.contains(&(i as u32)){
            *value = None;
        }
    }
}

// alive says which half is CURRENTLY alive. You must copy to the other half.
// 0 for left side currently in use, 1 for right side currently in use
pub fn stop_and_copy(mem: &mut Memory, alive: u32) -> () {
    let total = mem.heap.len();
    let mid = total / 2;
    let (start, end) = if alive == 0 { (0, mid) } else { (mid, 0) };
    let valid_range = start as u32..(start + mid) as u32;
    let mut seen = HashSet::new();
    let mut worklist = VecDeque::new();

    for frame in &mem.stack {
        for &idx in frame{
            if valid_range.contains(&idx) && seen.insert(idx) {
                worklist.push_back(idx);
            }
        }
    }
    while let Some(node) = worklist.pop_front(){
        if let Some(Some((_, children))) = mem.heap.get(node as usize) {
            for &child  in children{
                if seen.insert(child){
                    worklist.push_back(child);
                }
            }
        }
    }
    let live: Vec<u32> = seen.into_iter().collect();
    let mut c_heap = mem.heap.clone();
    let mut mapping: HashMap<u32, u32> = HashMap::new();
    let mut alloc_ptr = end as u32;

    for &old in  &live {
        if let Some(Some((label, _))) = mem.heap.get(old as usize) {
            c_heap[alloc_ptr as usize] = Some((label.clone(), Vec::new()));
            mapping.insert(old, alloc_ptr);
            alloc_ptr += 1;
        }
    }
    for &old in  &live {
        if let Some(Some((_, children))) = mem.heap.get(old as usize) {
            if let Some(Some((_, ref mut new_children))) = c_heap.get_mut(mapping[&old] as usize) {
                *new_children = children.iter()
                    .filter_map(|&x| mapping.get(&x).copied()).collect();
            }
        }
    }
    for frame in &mut mem.stack {
        for slot in frame.iter_mut(){
            if let Some(&new) = mapping.get(slot) {
                *slot = new;
            }
        }
    }
    for pos in end..(end + mid) {
        if !mapping.values().any(|&x| x == pos as u32){
            c_heap[pos] = None;
        }
    }
    mem.heap = c_heap;
}