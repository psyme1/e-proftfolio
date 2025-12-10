let photos = [];
let currentIndex = 0;
let slideshowInterval = null;
let dataLoaded = false;
const photo = document.getElementById("photo");
const statusInput = document.querySelector(".redText");
const statusBox = document.querySelector("fieldset input[readonly]");
const folderInput = document.querySelectorAll("input[type='text']")[1];
const commonInput = document.querySelectorAll("input[type='text']")[2];
const startInput = document.querySelectorAll("input[type='number']")[0];
const endInput = document.querySelectorAll("input[type='number']")[1];
const jsonInput = document.querySelectorAll("input[type='text']")[3];
const jsonButton = document.querySelectorAll("fieldset button")[1];

function showPhoto(index) {
    photo.src = photos[index];
    statusBox.value = photos[index];
}

function showError(message){
    statusInput.textContent = "Error:" + message;
    statusInput.style.color = "red";
}

function firstPhoto(){
    if(!dataLoaded) return showError("you must load data first");
    currentIndex = 0;
    showPhoto(currentIndex);
}

function lastPhoto(){
    if(!dataLoaded) return showError("you must load data first");
    currentIndex = photos.length - 1;
    showPhoto(currentIndex);
}

function prevPhoto() {
    if (!dataLoaded) return showError("you must load data first");
    currentIndex = (currentIndex - 1 + photos.length) % photos.length;
    showPhoto(currentIndex);
}

function nextPhoto() {
    if (!dataLoaded) return showError("you must load data first");
    currentIndex = (currentIndex + 1) % photos.length;
    showPhoto(currentIndex);
}

let buttons = document.querySelectorAll('#IterateSet button');
buttons[0].addEventListener("click", prevPhoto);
buttons[1].addEventListener("click", nextPhoto);
buttons[2].addEventListener("click", firstPhoto);
buttons[3].addEventListener("click", lastPhoto);

document.querySelector("button").addEventListener("click", function() {
    const folder = folderInput.value.trim();
    const common = commonInput.value.trim();
    const start = parseInt(startInput.value);
    const end = parseInt(endInput.value);
    if (start > end) {
        return showError("Invalid range");
    }
    photos=[];
    for (let i = start; i <= end; i++) {
        photos.push(`${folder}${common}${i}.jpg`);
    }
    currentIndex = 0;
    dataLoaded = true;
    statusInput.textContent = "Photo Viewer System";
    statusInput.style.color = "red";
});

jsonButton.addEventListener("click", async function() {
    const url = jsonInput.value.trim();
    try {
        const response = await fetch(url);
        const data = await response.json();
        photos = data.images.map(item => item.imageURL);
        if (photos.length === 0) return showError("No photos found in JSON");
        currentIndex = 0;
        dataLoaded = true;
        showPhoto(currentIndex);
        statusInput.textContent = "Photo Viewer System";
        statusInput.style.color = "red";
    } catch (error) {
        return showError("Failed to load JSON file");
    }
});

let slideButtons = document.querySelectorAll('#SlideShowSet button');
slideButtons[0].addEventListener("click", startSlideShow);
slideButtons[1].addEventListener("click", startRandomSlideShow);
slideButtons[2].addEventListener("click", stopSlideShow);

function startSlideShow() {
    if (!dataLoaded) return showError("you must load data first");
    stopSlideShow();
    slideshowInterval = setInterval(nextPhoto, 1000);
}

function stopSlideShow() {
    clearInterval(slideshowInterval);
}

function startRandomSlideShow() {
    if (!dataLoaded) return showError("you must load data first");
    stopSlideShow();
    slideshowInterval = setInterval(() => {
        currentIndex = Math.floor(Math.random() * photos.length);
        showPhoto(currentIndex);
    }, 1000);
}

const resetButton = document.querySelector("#marginBelow button");
resetButton.addEventListener("click", resetFormValues);

function resetFormValues() {
    statusBox.value = "";
    statusInput.textContent = "Photo Viewer System";
    statusInput.style.color = "red";
}
