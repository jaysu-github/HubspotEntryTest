import com.fasterxml.jackson.databind.ObjectMapper;
import model.*;

import java.util.*;

public class main {
    public static String GET_URL = "https://candidate.hubteam.com/candidateTest/v3/problem/dataset?userKey=8ec52529b42bc3c88ec626f19d5e";
    public static String POST_URL = "https://candidate.hubteam.com/candidateTest/v3/problem/result?userKey=8ec52529b42bc3c88ec626f19d5e";
    public static Countries COUNTRIES = new Countries();

    public static void main(String[] args) throws Exception {
        String content = httpHelper.sendGet(GET_URL);
        ObjectMapper objectMapper = new ObjectMapper();
        Partners partners = objectMapper.readValue(content, Partners.class);
        //1. Create a map <Country, Partner from this Country>
        Map<String, List<Partner>> partnerMap = createPartnerMap(partners);

        for (Map.Entry<String, List<Partner>> country : partnerMap.entrySet()) {
            //Create a calendar for this country. A calendar contains dates which have their available partners
            List<Partner> countryPartners = country.getValue();
            List<DateAndPartner> calendar = createCalendar(countryPartners);

            //Find best date and its maximum number of attending people
            int maxPeople = 0;
            String bestDate = "";
            for (int i = 0; i < calendar.size() - 1; i++) {
                DateAndPartner firstDay = calendar.get(i);
                DateAndPartner secondDay = calendar.get(i + 1);
                int size = 0;
                for (Partner p1 : firstDay.getPartnerList()) {
                    for (Partner p2 : secondDay.getPartnerList()) {
                        if (p1.equals(p2)) {
                            size++;
                        }
                    }
                }
                if (size > maxPeople) {
                    maxPeople = size;
                    bestDate = firstDay.getDate();
                }
            }

            //Add this country to the Countries object
            createCountryToReturn(maxPeople, country.getKey(), bestDate, calendar);
        }

        String response = objectMapper.writeValueAsString(COUNTRIES);
        httpHelper.sendPost(POST_URL, response);


    }

    /**
     * Add this country to the COUNTRIES object to  response
     * @param maxPeople number of partners can go to the meeting
     * @param countryName country name
     * @param bestDate best date to select for the meeting in the calendar
     * @param calendar calendar of this country
     */
    private static void createCountryToReturn(int maxPeople, String countryName, String bestDate, List<DateAndPartner> calendar) {
        Country returnCountry = new Country();
        returnCountry.setName(countryName);

        if (maxPeople == 0) {
            returnCountry.setAttendeeCount(0);
            returnCountry.setStartDate(bestDate);
        } else {
            //Create return object
            for (DateAndPartner date : calendar) {
                if (date.getDate().equals(bestDate)) {
                    returnCountry.setAttendeeCount(date.getPartnerList().size());
                    List<String> partnerList = new ArrayList<>();
                    for (Partner partner : date.getPartnerList()) {
                        partnerList.add(partner.getEmail());
                    }
                    returnCountry.setAttendees(partnerList);
                    returnCountry.setStartDate(bestDate);
                    COUNTRIES.getCountries().add(returnCountry);
                    break;
                }
            }
        }
    }

    /**
     * Create a map in which keys are COUNTRIES, values are partners from their COUNTRIES.
     *
     * @param partners: List of partners parsed from JSON input file
     * @return Map<Country, Partners>
     */
    private static Map<String, List<Partner>> createPartnerMap(Partners partners) {
        Map<String, List<Partner>> partnerMap = new HashMap<>();
        for (Partner partner : partners.getPartners()) {
            if (partnerMap.get(partner.getCountry()) != null) {
                partnerMap.get(partner.getCountry()).add(partner);
            } else {
                List<Partner> partnerList = new ArrayList<>();
                partnerList.add(partner);
                partnerMap.put(partner.getCountry(), partnerList);
            }
        }
        return partnerMap;
    }

    /**
     * Create a calendar of one country
     * The calendar contains dates. Each date has its own  attendee
     * @param countryPartners partners from this country
     * @return calendar
     */
    private static List<DateAndPartner> createCalendar(List<Partner> countryPartners) {
        List<DateAndPartner> calendar = new ArrayList<>();
        //Get all available dates from all partners in this country
        Set<String> dates = new HashSet<>();
        for (Partner partner : countryPartners) {
            dates.addAll(partner.getAvailableDates());
        }
        for (String date : dates) {
            DateAndPartner dateAndPartner = new DateAndPartner();
            dateAndPartner.setDate(date);
            calendar.add(dateAndPartner);
        }

        //Sort the calendar
        Collections.sort(calendar, (o1, o2) -> {
            Integer day1 = Integer.valueOf(o1.getDate().replace("-", ""));
            Integer day2 = Integer.valueOf(o2.getDate().replace("-", ""));
            if (day1 > day2) return 1;
            else return -1;
        });

        //Add the partners to the partnerList on every date.
        for (Partner partner : countryPartners) {
            List<String> availableDates = partner.getAvailableDates();
            for (String date : availableDates) {
                for (DateAndPartner dateAndPartner : calendar) {
                    if (dateAndPartner.getDate().equals(date)) {
                        dateAndPartner.getPartnerList().add(partner);
                        break;
                    }
                }
            }
        }
        return calendar;
    }
}
