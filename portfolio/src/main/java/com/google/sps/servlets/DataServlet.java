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

    for (Entity review : allReviews.asIterable()) {
      String reviewOutput = (String) review.getProperty("review");
      reviews.add(reviewOutput);
    }

    String reviewJson = new Gson().toJson(reviews);
    response.setContentType("text/html;");
    response.getWriter().println(reviewJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the input from the form. DefaultValue is "" to ensure consistent "fails" across all calls to getParameter
    // TODO: Make checks if name/input = DefaultValue, then don't add comment.
    String name = getParameter(request, "reviewer-name", "");
    String input = getParameter(request, "reviewer-input", "");
    String review = name + " said: \n" + input;

    Entity reviewEntity = new Entity("Task");
    reviewEntity.setProperty("review", review);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(reviewEntity);
    response.sendRedirect("/index.html");
  }

  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
