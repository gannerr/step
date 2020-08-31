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


  /**
   * A comparator for sorting Event's timeranges by their start time in ascending order.
   */
  public static final Comparator<Event> SORT_BY_START = new Comparator<Event>() {
    @Override
    public int compare(Event a, Event b) {
      return Long.compare(a.getWhen().start(), b.getWhen().start());
    }
  };


  /**
   * Filtering and sorting events based on their start time in ascending order
   */
  public List<Event> sortEvents(Collection<Event> events, MeetingRequest request) {
    List<Event> eventsWithPossibleConflicts = new ArrayList<Event>();
    for (Event event : events) {
      if (!Collections.disjoint(request.getAttendees(), event.getAttendees())) {
        eventsWithPossibleConflicts.add(event);
      }
    }

    Collections.sort(eventsWithPossibleConflicts, SORT_BY_START);
    return eventsWithPossibleConflicts;
  }


  /**
   * Merging overlapping events
   */
  public List<TimeRange> mergeEvents(Collection<Event> events, MeetingRequest request, List<Event> eventsWithPossibleConflicts) {
    List<TimeRange> unavailableTimeRanges = new ArrayList<TimeRange>();

    //if there are more than 2 events, then there may be possible conflicts that require merging
    //otherwise, there will be 1 event (0 events case has already been resolved) 
    //and we don't have to do any merging
    if (eventsWithPossibleConflicts.size() == 1) {
      unavailableTimeRanges.add(eventsWithPossibleConflicts.get(0).getWhen());
    } else {
      for (int i = 0; i < eventsWithPossibleConflicts.size()-1; i++) {
        Event event1 = eventsWithPossibleConflicts.get(i);
        Event event2 = eventsWithPossibleConflicts.get(i+1);
        if (event1.getWhen().contains(event2.getWhen())) {
          unavailableTimeRanges.add(event1.getWhen());
        } else if (event1.getWhen().overlaps(event2.getWhen())) {
          unavailableTimeRanges.add(TimeRange.fromStartEnd(event1.getWhen().start(), event2.getWhen().end(), false));
        } else {
          unavailableTimeRanges.add(event1.getWhen());
          unavailableTimeRanges.add(event2.getWhen());
        }
      } 
    }

    return unavailableTimeRanges;
  }
  
  //convert blocked times to available times
  private Collection<TimeRange> findAvailableTimeRanges(Collection<Event> events, MeetingRequest request, List<TimeRange> unavailableTimeRanges) {
    Collection<TimeRange> possibleTimesForMeeting = new ArrayList();
    int start = 0;

    //Prelim check to see if we can host a meeting from start of day to start of first event
    if (unavailableTimeRanges.get(0).start() != TimeRange.START_OF_DAY
      && unavailableTimeRanges.get(0).start() >= request.getDuration()) {
      possibleTimesForMeeting.add(TimeRange.fromStartEnd(start, unavailableTimeRanges.get(0).start(), false));
    }
    //check if there is space for a meeting from end of first event to start of last event
    //only occurs if we have more than 2 events to inspect, as otherwise it's just
    //start of day to event and event to end of day
    if (unavailableTimeRanges.size() >= 2) {
      for (int i = 0; i < unavailableTimeRanges.size()-1; i++) {
        TimeRange event1 = unavailableTimeRanges.get(i);
        TimeRange event2 = unavailableTimeRanges.get(i+1);
        TimeRange goodTime = TimeRange.fromStartEnd(event1.end(), event2.start(), false);
        if (goodTime.duration() >= request.getDuration()) {
          possibleTimesForMeeting.add(goodTime);
        }
        start = event1.end();
      }
    }
    //check if there is space for a meeting from end of last event to end of day
    if (unavailableTimeRanges.get(unavailableTimeRanges.size()-1).end() < TimeRange.END_OF_DAY
      && TimeRange.END_OF_DAY - unavailableTimeRanges.get(unavailableTimeRanges.size()-1).end() >= request.getDuration()) {
      possibleTimesForMeeting.add(TimeRange.fromStartEnd(unavailableTimeRanges.get(unavailableTimeRanges.size()-1).end(), TimeRange.END_OF_DAY, true));
    }

    return possibleTimesForMeeting;
  }

  /**
  * Returns a Collection of TimeRanges that will contain the best times to  host a meeting
  *
  * @param  events  every current event that a person is attending, which will not be good times to host a meeting
  * @param  request the requested meeting, which specifies the people required to attend and how long it's for
  * @return         a Collection of TimeRanges that will contain the best times to  host a meeting
  */
  public Collection<TimeRange> buildQuery(Collection<Event> events, MeetingRequest request) {
    List<Event> eventsWithPossibleConflicts = new ArrayList<Event>();
    // sort events by start time
    eventsWithPossibleConflicts = sortEvents(events, request);
    
    //ignore people not attending
    if (eventsWithPossibleConflicts.isEmpty()) {
      Collection<TimeRange> possibleTimesForMeeting = Arrays.asList(TimeRange.WHOLE_DAY);
      return possibleTimesForMeeting;
    }

    List<TimeRange> unavailableTimeRanges = new ArrayList<TimeRange>();
    //merge event times
    unavailableTimeRanges = mergeEvents(events, request, eventsWithPossibleConflicts);
    
    Collection<TimeRange> possibleTimesForMeeting = new ArrayList<TimeRange>();
    //divide time into before event & after event
    possibleTimesForMeeting = findAvailableTimeRanges(events, request, unavailableTimeRanges);

    //ignores people not attending
    if (eventsWithPossibleConflicts.isEmpty()) {
      possibleTimesForMeeting.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, true));
    }

    //considers the case wherein we have both mandatory and optional attendees.
    //If one or more time slots exists so that both mandatory and optional attendees can attend, 
    //return those time slots. Otherwise, return the time slots that fit just the mandatory attendees.
    if (!possibleTimesForMeeting.isEmpty() && !request.getOptionalAttendees().isEmpty()) {
      MeetingRequest newRequest = new MeetingRequest(request.getOptionalAttendees(), request.getDuration());
      Collection<TimeRange> optionalpossibleTimesForMeeting = buildQuery(events, newRequest);
      Collection<TimeRange> bestTimes = new ArrayList<TimeRange>();

      for (TimeRange mandatoryTime : possibleTimesForMeeting) {
        for (TimeRange optionalTime : optionalpossibleTimesForMeeting) {
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

    return possibleTimesForMeeting;
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) { 
      Collection<TimeRange> possibleTimesForMeeting = Collections.emptyList();
      return possibleTimesForMeeting;
    } 
    
    if (events.isEmpty()) {
      Collection<TimeRange> possibleTimesForMeeting = Arrays.asList(TimeRange.WHOLE_DAY);
      return possibleTimesForMeeting;
    } 
    
    if (request.getAttendees().isEmpty()) {
      if (request.getOptionalAttendees().isEmpty()) {
        Collection<TimeRange> possibleTimesForMeeting = Arrays.asList(TimeRange.WHOLE_DAY);
        return possibleTimesForMeeting;
      }

      MeetingRequest newRequest = new MeetingRequest(request.getOptionalAttendees(), request.getDuration());
      request = newRequest;
    }

    Collection<TimeRange> possibleTimesForMeeting = buildQuery(events, request);
    return possibleTimesForMeeting;
  }
}
