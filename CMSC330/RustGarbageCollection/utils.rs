use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;


fn read_lines<P>(filename: P) -> io::Result<io::Lines<io::BufReader<File>>>
where P: AsRef<Path>, {
    let file = File::open(filename)?;
    Ok(io::BufReader::new(file).lines())
}

pub fn file_to_str_vec(filename: &str) ->  Vec<String> {

    let mut str_vec: Vec<String> = vec![];

    if let Ok(lines) = read_lines(filename) {
        for line in lines.flatten() {
            str_vec.push(line.clone());
        }
    };

    return str_vec;

}

pub fn int_of_string(str: String) -> u32 {
    let num:u32 = str.parse().expect("String is not an integer");
    return num;
}