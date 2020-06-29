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

package com.google.sps.servlets;

import java.io.IOException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/reviews")
public class DataServlet extends HttpServlet {
  @Override
  public void init() {
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query reviewQuery = new Query("Task").addSort("review", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery allReviews = datastore.prepare(reviewQuery);
    ArrayList<String> reviews = new ArrayList<String>();

    int maxComments = getMaxReviews(request);

    if (maxComments == -1) {
      response.setContentType("text/html");
      response.getWriter().println("Please enter a positive integer");
      return;
    }

    int commentCount = 0;
    for (Entity review : allReviews.asIterable()) {
      if(commentCount < maxComments){
        String reviewOutput = (String) review.getProperty("review");
        reviews.add(reviewOutput);
      }
      commentCount++;
    }

    String reviewJson = new Gson().toJson(reviews);
    response.setContentType("application/json;");
    response.getWriter().println(reviewJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form. If we have null/empty input, abort.
    String name = request.getParameter("reviewer-name");
    String input = request.getParameter("reviewer-input");

    if(name.length() == 0 || input.length() == 0) {
        response.sendRedirect("/reviews.html");
        return;
    }

    String review = name + " said: \n" + input;
    Entity reviewEntity = new Entity("Task");
    reviewEntity.setProperty("review", review);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(reviewEntity);
    response.sendRedirect("/reviews.html");
  }

  private int getMaxReviews(HttpServletRequest request) {
    // Get the input from the form.
    String maxReviews = request.getParameter("max-reviews");

    // Convert the input to an int.
    int maxReview;
    try {
      maxReview = Integer.parseInt(maxReviews);
    } catch (NumberFormatException e) {
      System.err.println("Could not convert to int: " + maxReviews);
      return -1;
    }

    // Check that the input is positive
    if (maxReview < 1) {
      System.err.println("Surely you want to see SOME reviews");
      return -1;
    }

    return maxReview;
  }
}
