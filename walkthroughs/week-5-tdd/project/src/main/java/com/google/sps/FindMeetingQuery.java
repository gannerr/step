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

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class FindMeetingQuery {
  public static final Comparator<Event> SORT_BY_START = new Comparator<Event>() {
    @Override
    public int compare(Event a, Event b) {
      return Long.compare(a.getWhen().start(), b.getWhen().start());
    }
  };

  public List<Event> sortEvents(Collection<Event> events, MeetingRequest request) {
    List<Event> eventsList = new ArrayList<Event>();
    for (Event event : events) {
      if (request.getAttendees().containsAll(event.getAttendees())) {
        eventsList.add(event);
      }
    }

    Collections.sort(eventsList, SORT_BY_START);
    return eventsList;
  }

  public List<TimeRange> mergeEvents(Collection<Event> events, MeetingRequest request, List<Event> eventsList) {
    List<TimeRange> mergedEventsList = new ArrayList<TimeRange>();
    if (eventsList.size() >= 2) {
      for (int i = 0; i < eventsList.size()-1; i++) {
        Event event1 = eventsList.get(i);
        Event event2 = eventsList.get(i+1);
        if (event1.getWhen().contains(event2.getWhen())) {
          mergedEventsList.add(event1.getWhen());
        } else if (event1.getWhen().overlaps(event2.getWhen())) {
          mergedEventsList.add(TimeRange.fromStartEnd(event1.getWhen().start(), event2.getWhen().end(), false));
        } else {
          mergedEventsList.add(event1.getWhen());
          mergedEventsList.add(event2.getWhen());
        }
      } 
    } else {
      mergedEventsList.add(eventsList.get(0).getWhen());
    }

    return mergedEventsList;
  }
  
  public Collection<TimeRange> sectionTime(Collection<Event> events, MeetingRequest request, List<TimeRange> mergedEventsList) {
    Collection<TimeRange> expected = new ArrayList();

    int start = 0, end = 0;
    if (mergedEventsList.size() >= 2) {
      if (mergedEventsList.get(0).start() != TimeRange.START_OF_DAY) {
        expected.add(TimeRange.fromStartEnd(start, mergedEventsList.get(0).start(), false));
      }
      for (int i = 0; i < mergedEventsList.size()-1; i++) {
        TimeRange event1 = mergedEventsList.get(i);
        TimeRange event2 = mergedEventsList.get(i+1);
        TimeRange secondHalf = TimeRange.fromStartEnd(event1.end(), event2.start(), false);
        if (event2.start() - event1.end() >= request.getDuration()) {
          expected.add(secondHalf);
        }
        start = event1.end();
      }

      if (mergedEventsList.get(mergedEventsList.size()-1).end() < TimeRange.END_OF_DAY) {
        expected.add(TimeRange.fromStartEnd(mergedEventsList.get(mergedEventsList.size()-1).end(), TimeRange.END_OF_DAY, true));
      }
    } else {
      if (mergedEventsList.get(0).start() != TimeRange.START_OF_DAY) {
        expected.add(TimeRange.fromStartEnd(start, mergedEventsList.get(0).start(), false));
      }
      if (mergedEventsList.get(0).end() < TimeRange.END_OF_DAY) {
        expected.add(TimeRange.fromStartEnd(mergedEventsList.get(0).end(), TimeRange.END_OF_DAY, true));
      }
    }

    return expected;
  }

  public Collection<TimeRange> buildQuery(Collection<Event> events, MeetingRequest request) {
    // sort events by start time
    List<Event> eventsList = new ArrayList<Event>();
    eventsList = sortEvents(events, request);
    
    //ignore people not attending
    if (eventsList.isEmpty()) {
      Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
      return expected;
    }

    //merge event times
    List<TimeRange> mergedEventsList = new ArrayList<TimeRange>();
    mergedEventsList = mergeEvents(events, request, eventsList);
    
    //divide time into before event & after event
    Collection<TimeRange> expected = new ArrayList<TimeRange>();
    expected = sectionTime(events, request, mergedEventsList);

    //ignores people not attending
    if (eventsList.isEmpty()) {
      expected.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true));
    }

    //considers the case wherein we have both mandatory and optional attendees.
    //If one or more time slots exists so that both mandatory and optional attendees can attend, 
    //return those time slots. Otherwise, return the time slots that fit just the mandatory attendees.
    if (!expected.isEmpty() && !request.getOptionalAttendees().isEmpty()) {
      MeetingRequest newRequest = new MeetingRequest(request.getOptionalAttendees(), request.getDuration());
      Collection<TimeRange> optionalExpected = buildQuery(events, newRequest);
      Collection<TimeRange> bestTimes = new ArrayList<TimeRange>();

      for (TimeRange mandatoryTime : expected) {
        for (TimeRange optionalTime : optionalExpected) {
          if (optionalTime.contains(mandatoryTime)) {
            bestTimes.add(mandatoryTime);
            break;
          }
        }
      }
      if (!bestTimes.isEmpty()) {
        return bestTimes;
      }
    }

    return expected;
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) { 
      Collection<TimeRange> expected = Arrays.asList();
      return expected;
    } else if (events.isEmpty()) {
      Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
      return expected;
    } else if (request.getAttendees().isEmpty()) {
      if(request.getOptionalAttendees().isEmpty()) {
        Collection<TimeRange> expected = Arrays.asList(TimeRange.WHOLE_DAY);
        return expected;
      } else {
        MeetingRequest newRequest = new MeetingRequest(request.getOptionalAttendees(), request.getDuration());
        request = newRequest;
        Collection<TimeRange> expected = buildQuery(events, request);
        return expected;
      }
    } else {
      Collection<TimeRange> expected = buildQuery(events, request);
      return expected;
    }
  }
}
