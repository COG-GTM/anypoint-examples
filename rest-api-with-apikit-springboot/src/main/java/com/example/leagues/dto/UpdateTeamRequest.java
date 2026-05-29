package com.example.leagues.dto;

/**
 * Request body for updating a team (PUT /api/teams/{teamId}).
 *
 * <p>Maps to the MuleSoft example's {@code request.UpdateTeam}. All fields are
 * optional; only the non-null fields are applied (partial update).
 */
public class UpdateTeamRequest {

    private String name;
    private String homeCity;
    private String stadium;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHomeCity() {
        return homeCity;
    }

    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
    }

    public String getStadium() {
        return stadium;
    }

    public void setStadium(String stadium) {
        this.stadium = stadium;
    }
}
