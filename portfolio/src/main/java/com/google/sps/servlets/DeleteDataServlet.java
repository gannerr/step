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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/delete-data")
public class DeleteDataServlet extends HttpServlet {
  @Override
  public void init() {}

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
    response.setContentType("application/json;");
    response.getWriter().println(reviewJson);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query reviewQuery = new Query("Task").addSort("review", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery allReviews = datastore.prepare(reviewQuery);
    ArrayList<Key> keys = new ArrayList<>();
    for(Entity review : allReviews.asIterable()) {
      keys.add(review.getKey());
    }
    datastore.delete(keys);
    response.sendRedirect("/reviews.html");
  }
}
