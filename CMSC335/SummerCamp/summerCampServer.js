const express = require('express');
const { read } = require('fs');
const readline = require('readline');
const path = require("path");
require("dotenv").config({
   path: path.resolve(__dirname, "credentialsDontPost/.env"),
});
const { MongoClient, ServerApiVersion } = require("mongodb");

const app = express();

const databaseName = "CMSC335DB";
const uri = process.env.MONGO_CONNECTION_STRING;
const collectionName = "campApplicants";

const port = process.argv[2];

app.set('view engine', 'ejs');
app.set("views", __dirname + "/");
app.use(express.urlencoded({ extended: false }));
app.use(express.static(path.join(__dirname, "public")));

(async () => {
    const client = new MongoClient(uri, {
            serverApi: {version: ServerApiVersion.v1}});
    try {
        await client.connect();
        const database = client.db(databaseName);
        const collection = database.collection(collectionName);
        server = app.listen(port, () => {
            console.log(`Web server started and running at http://localhost:${port}`);
            process.stdout.write("Stop to shutdown the server: ");
        });

        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });

        rl.on('line', (input) => {
            if (input.trim().toLowerCase() === 'stop') {
                process.stdout.write('Shutting down the server');
                rl.close();
                process.exit(0);
            }
        });

        app.get('/', (req, res) => {
            res.render('index');
        });

        app.get('/apply', (req, res) => {
            res.render('apply');
        });

        app.post('/apply', async (req, res) => {
            const parsed = parseFloat(req.body.gpa);
            const doc = {
                name: req.body.name,
                email: req.body.email,
                gpa: parsed,
                background: req.body.background
            };
            try{
                const result = await collection.insertOne(doc);
                const now = new Date();
                res.render('processApplication', { applicant: doc, currentTime: now});
            }catch (err) {
                console.error("Error inserting document into MongoDB:", err);
            }
        });

        app.get('/find', async (req, res) => {
            res.render('find', {result: null});
        });

        app.get("/reviewApplication", (req, res) => {
            res.render("reviewApplication");
        });

        app.get("/adminGFA", (req, res) => {
            res.render("adminGFA");
        });

        app.get("/adminRemove", (req, res) => {
            res.render("adminRemove");
        });

        app.post("/reviewApplication", async (req, res) => {
            const email = req.body.email;
            try {
                const applicant = await collection.findOne({ email: email });
                const now = new Date();
                res.render("processReviewApplication", { applicant, currentTime: now });
            }
            catch (err) {

            }
        });

        app.post('/processAdminGFA', async (req, res) => {
            const gpaThreshold = parseFloat(req.body.gpa);
            try{
                const docs = await collection.find({ gpa: { $gte: gpaThreshold } }).project({name: 1, gpa: 1, _id: 0}).toArray();
                res.render('processAdminGFA', { applicants: docs, gpaThreshold });
            }catch(err){
                console.error(err);
            }
        });

        app.post('/processAdminRemove', async (req, res) => {
            try{
                const result = await collection.deleteMany({});
                res.render('processAdminRemove', { deletedCount: result.deletedCount });
            }catch(err){

            }
        });

    }catch (err) {
        console.error("Error connecting to MongoDB:", err);
        process.exit(1);
    }
})();
