/*
 * Kevin Bacon Machine – frontend
 *
 * Sends the searched name to the Java server, then draws the path to Kevin Bacon
 * (actor -> movie -> actor ...) without reloading the page.
 */

const searchForm = document.getElementById("searchForm");
const actorInput = document.getElementById("actorInput");
const message = document.getElementById("message");
const resultArea = document.getElementById("resultArea");
const loadingIndicator = document.getElementById("loadingIndicator");
const datasetInfo = document.getElementById("datasetInfo");


/* Sends the search to the server and shows the result. */
function searchActor(event) {
    event.preventDefault();

    const actorName = actorInput.value.trim();
    if (actorName === "") {
        return;
    }

    loadingIndicator.hidden = false;
    message.textContent = "";
    resultArea.innerHTML = "";

    const request = new XMLHttpRequest();
    request.open("GET", "/api/bacon?actor=" + encodeURIComponent(actorName), true);

    request.addEventListener("load", function () {
        loadingIndicator.hidden = true;

        if (request.status === 200) {
            showResults(JSON.parse(request.responseText));
        } else {
            message.textContent = "Error " + request.status + ": " + request.responseText;
        }
    });

    request.addEventListener("error", function () {
        loadingIndicator.hidden = true;
        message.textContent = "Could not reach the server.";
    });

    request.send();
}


/* Clones the actor template, fills in the name and starts loading the image.
   textContent is used so that names are never interpreted as HTML. */
function createActorNode(actorName) {
    const clone = document.getElementById("actorTemplate").content.cloneNode(true);
    const actorNode = clone.querySelector(".actorNode");

    clone.querySelector(".actorName").textContent = actorName;
    loadActorImage(actorName, actorNode);

    return actorNode;
}


/* Clones the movie template and fills in title and year. */
function createMovieNode(movieTitle, movieYear) {
    const clone = document.getElementById("movieTemplate").content.cloneNode(true);
    const movieNode = clone.querySelector(".movieNode");

    movieNode.textContent = movieTitle + " (" + movieYear + ")";

    return movieNode;
}


function createArrow() {
    const arrow = document.createElement("div");
    arrow.classList.add("arrow");
    arrow.textContent = "↓";
    return arrow;
}


/* Shows one card per matching actor. */
function showResults(results) {
    resultArea.innerHTML = "";

    if (results.length === 0) {
        message.textContent = "No actors found. Check the spelling and try again.";
        return;
    }

    for (const result of results) {
        const card = document.createElement("article");

        const name = document.createElement("h2");
        name.textContent = result.name;

        const baconNumber = document.createElement("p");
        if (result.baconNumber === -1) {
            baconNumber.textContent = "No connection to Kevin Bacon was found.";
            baconNumber.classList.add("notConnected");
        } else {
            baconNumber.textContent = "Bacon number: " + result.baconNumber;
        }

        card.appendChild(name);
        card.appendChild(baconNumber);
        showPath(result.path, card);

        resultArea.appendChild(card);
    }
}


/* Draws the path from the actor to Kevin Bacon and fades it in step by step. */
function showPath(path, container) {
    if (path.length === 0) {
        return;
    }

    const pathContainer = document.createElement("div");
    pathContainer.classList.add("pathContainer");

    pathContainer.appendChild(createActorNode(path[0].fromActor));

    for (const step of path) {
        pathContainer.appendChild(createArrow());
        pathContainer.appendChild(createMovieNode(step.movieTitle, step.movieYear));
        pathContainer.appendChild(createArrow());
        pathContainer.appendChild(createActorNode(step.toActor));
    }

    container.appendChild(pathContainer);

    const items = pathContainer.children;
    for (let i = 0; i < items.length; i++) {
        items[i].style.display = "none";
        $(items[i]).delay(i * 500).fadeIn(600);
    }
}


/* Asks the server for an image URL; the image stays hidden if there is none. */
function loadActorImage(actorName, actorNode) {
    const request = new XMLHttpRequest();
    request.open("GET", "/api/actor-image?name=" + encodeURIComponent(actorName), true);

    request.addEventListener("load", function () {
        if (request.status === 200) {
            const image = actorNode.querySelector(".actorImage");
            image.src = request.responseText;
            image.alt = "Photo of " + actorName;
            image.hidden = false;
        }
    });

    request.send();
}


/* Shows the size of the loaded dataset in the footer. */
function loadStats() {
    const request = new XMLHttpRequest();
    request.open("GET", "/api/stats", true);

    request.addEventListener("load", function () {
        if (request.status === 200) {
            const stats = JSON.parse(request.responseText);
            const format = new Intl.NumberFormat("en-US");
            datasetInfo.textContent =
                format.format(stats.actors) + " actors · " +
                format.format(stats.movies) + " movies" +
                (stats.dataset === "demo" ? " (demo dataset)" : "");
        }
    });

    request.send();
}


searchForm.addEventListener("submit", searchActor);
loadStats();
