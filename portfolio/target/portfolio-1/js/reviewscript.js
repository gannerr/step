function fetchLoginStatus() {
  fetch('/login').then(response => response.text()).then((loginStatus) => {
    //Because loginStatus will return either the logout / login message
    //we can just check the length of return message to ascertain if
    //we are logged in or out
    if(loginStatus.length < 100) {
      document.getElementById("submit-reviews-form").style.visibility = "hidden";
      document.getElementById("check-reviews-form").style.visibility = "hidden";
      window.location = "/login";
    }
    else {
      document.getElementById("submit-reviews-form").style.visibility = "visible";
      document.getElementById("check-reviews-form").style.visibility = "visible";
    }
  });
}

window.addEventListener('load', fetchLoginStatus);
