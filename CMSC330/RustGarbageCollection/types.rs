#[derive(Debug, PartialEq)]
pub struct Memory {
    pub stack:Vec<Vec<u32>>,
    pub heap: Vec<Option<(String, Vec<u32>)>>,
}

#[derive(Debug, PartialEq)]
pub struct RefCountMem {
    pub stack: Vec<Vec<u32>>,
    pub heap: Vec<(Option<Vec<u32>>, u32)>,
}
