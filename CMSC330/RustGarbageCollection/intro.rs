//! This is the file where you will write your code.
#![allow(unused_imports)]

use std::{
    collections::HashMap,
    fs::File,
    io::{BufRead, BufReader},
    ops::Range,
    iter
};

use regex::Regex;

/// Sums the numbers from 1 to `n`, where `n` is an arbitrary positive
/// integer. 
/// # Example
/// 
/// ```
// # use project_6::gauss;
// assert_eq!(gauss(10), Some(55));
/// ```

pub fn gauss(n: i32) -> Option<i32> {
    if n <= 0 {
        None
    }else{
        Some(n*(n+1)/2)
    }

}


/// This function takes in an integer and returns the equivalent binary string

pub fn to_binstring(num: u32) -> String {
    format!("{:b}", num)
}

/// This function takes in an iterable of integers,
/// and counts how many elements in the iterator are within the given range

pub fn in_range(itemlist: impl IntoIterator<Item = i32>, range: Range<i32>) -> usize {
    itemlist.into_iter().filter(|&i| range.contains(&i)).count()
}

/// Given an iterator over mutable strings, this function will capitalize the
/// first letter of each word in the iterator, in place.

pub fn capitalize_words<'a>(wordlist: impl IntoIterator<Item = &'a mut String>) {
    for word in wordlist {
        let first = word.chars().next();
        if first.is_some() {
            let cha = first.unwrap();
            let cap = cha.to_uppercase().to_string();
            word.replace_range(0..cha.len_utf8(), &cap);
        }
    }
}

/// Given a txt file, parse and return the items sold by a vending machine, along
/// with their prices.

pub fn read_prices(filename: &str) -> Option<HashMap<String, u32>> {

    let file = File::open(filename);
    if !file.is_ok() {
        return None;
    }
    let file = file.unwrap();
    let reader = BufReader::new(file);
    let mut res:HashMap<String, u32> = HashMap::new();
    let re = Regex::new(r"^\s*([a-zA-Z ]+)\s*;\s*(\d+)\s*(cents|c)\s*$").unwrap();
    for line in reader.lines(){
        let line = line.unwrap();
        if line.starts_with(';') {
            continue;
        }
        if let Some(caps) = re.captures(&line) {
            let name =  caps.get(1).unwrap().as_str().trim().to_string();
            let price = caps.get(2).unwrap().as_str().parse::<u32>().unwrap();
            if res.contains_key(&name) {
                return None;
            }
            if price < 1 || price > 50{
                return None;
            }
            res.insert(name, price);
        }else{
            return None;
        }
    }

    Some(res)
}

// STUDENT TESTS:
#[test]
fn student_test1() {
    assert_eq!(gauss(3), Some(6));
    assert_eq!(gauss(10), Some(55));
    assert_eq!(gauss(-1), None);
    assert_eq!(gauss(0), None);
    /*let map = HashMap::from([
        ("a".to_owned(), 20), ("b".to_owned(), 15), ("c".to_owned(), 30), ("e".to_owned(), 45)
    ]);
    assert_eq!(read_prices("src/student_test.txt"), Some(map));*/
}
