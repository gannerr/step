// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

function expandImage(imgs) {
  var expandImg = document.getElementById("expandedImg");
  expandImg.src = imgs.src;
  expandImg.parentElement.style.display = "block";
  window.scrollTo(0,document.body.scrollHeight);
}

function getReviews() {
  var query = document.getElementById("max-comments").value;
  fetch('/reviews?max-reviews='+query).then(response => response.json()).then((reviews) => {
    document.getElementById('reviews-container').innerText = "";
    for(let review of reviews) {
      document.getElementById('reviews-container').innerText += review + '\n\n';
    }
  });
}

function deleteReviews() {
  fetch('/delete-data').then(response => response.json()).then((reviews) => {
    document.getElementById('reviews-container').innerText = "";
    for(let review of reviews) {
      document.getElementById('reviews-container').innerText += review + '\n\n';
    }
  });
}

/** Creates a map and adds it to the page. */
function createMap() {
  const map = new google.maps.Map(
    document.getElementById('map'), {
      center: {lat: 42.1784, lng: -87.9979},
      zoom: 11,
      styles: [
        {elementType: 'geometry', stylers: [{color: '#242f3e'}]},
        {elementType: 'labels.text.stroke', stylers: [{color: '#242f3e'}]},
        {elementType: 'labels.text.fill', stylers: [{color: '#746855'}]},
        {
            featureType: 'administrative.locality',
            elementType: 'labels.text.fill',
            stylers: [{color: '#d59563'}]
        },
        {
            featureType: 'poi',
            elementType: 'labels.text.fill',
            stylers: [{color: '#d59563'}]
        },
        {
            featureType: 'poi.park',
            elementType: 'geometry',
            stylers: [{color: '#263c3f'}]
        },
        {
            featureType: 'poi.park',
            elementType: 'labels.text.fill',
            stylers: [{color: '#6b9a76'}]
        },
        {
            featureType: 'road',
            elementType: 'geometry',
            stylers: [{color: '#38414e'}]
        },
        {
            featureType: 'road',
            elementType: 'geometry.stroke',
            stylers: [{color: '#212a37'}]
        },
        {
            featureType: 'road',
            elementType: 'labels.text.fill',
            stylers: [{color: '#9ca5b3'}]
        },
        {
            featureType: 'road.highway',
            elementType: 'geometry',
            stylers: [{color: '#746855'}]
        },
        {
            featureType: 'road.highway',
            elementType: 'geometry.stroke',
            stylers: [{color: '#1f2835'}]
        },
        {
            featureType: 'road.highway',
            elementType: 'labels.text.fill',
            stylers: [{color: '#f3d19c'}]
        },
        {
            featureType: 'transit',
            elementType: 'geometry',
            stylers: [{color: '#2f3948'}]
        },
        {
            featureType: 'transit.station',
            elementType: 'labels.text.fill',
            stylers: [{color: '#d59563'}]
        },
        {
            featureType: 'water',
            elementType: 'geometry',
            stylers: [{color: '#17263c'}]
        },
        {
            featureType: 'water',
            elementType: 'labels.text.fill',
            stylers: [{color: '#515c6d'}]
        },
        {
            featureType: 'water',
            elementType: 'labels.text.stroke',
            stylers: [{color: '#17263c'}]
        }
        ]
    });

    setMarkers(map);
}

window.onload = createMap;

// Data for the markers consisting of a name, a LatLng and a zIndex for the
// order in which these markers should display on top of each other.
var restaurants = [
  {
    "name": 'Bonta', 
    "latitude": 42.1992, 
    "longitude": -87.9333, 
    "zIndex": 4
  },
  {
    "name": 'Walker Bros', 
    "latitude": 42.194679260253906, 
    "longitude": -87.92904663085938, 
    "zIndex": 5
  },
  {
    "name": 'Lou Malnati\'s', 
    "latitude": 42.15217208862305, 
    "longitude": -87.96086883544922, 
    "zIndex": 3
  },
  {
    "name": 'Joanie\'s', 
    "latitude": 42.178654, 
    "longitude": -87.9975738, 
    "zIndex": 2
  },
  {
    "name": 'Portillo\'s', 
    "latitude": 42.2409503, 
    "longitude": -87.9474605, 
    "zIndex": 1
  }
];

function setMarkers(map) {
  // Adds markers to the map.

  // Marker sizes are expressed as a Size of X,Y where the origin of the image
  // (0,0) is located in the top left of the image.

  // Origins, anchor positions and coordinates of the marker increase in the X
  // direction to the right and in the Y direction down.
  var image = {
    url: '/images/beachflag.png',
    // This marker is 20 pixels wide by 32 pixels high.
    size: new google.maps.Size(20, 32),
    // The origin for this image is (0, 0).
    origin: new google.maps.Point(0, 0),
    // The anchor for this image is the base of the flagpole at (0, 32).
    anchor: new google.maps.Point(0, 32)
  };

  // Shapes define the clickable region of the icon. The type defines an HTML
  // <area> element 'poly' which traces out a polygon as a series of X,Y points.
  // The final coordinate closes the poly by connecting to the first coordinate.
  var shape = {
    coords: [1, 1, 1, 20, 18, 20, 18, 1],
    type: 'poly'
  };
  for (let restaurant of restaurants) {
    var marker = new google.maps.Marker({
      position: {lat: restaurant.latitude, lng: restaurant.longitude},
      map: map,
      icon: image,
      shape: shape,
      title: restaurant.name,
      zIndex: restaurant.zIndex
    });
    
    marker.setMap(map);
  }
}
