package com.example.leagues.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a team (POST /api/teams).
 *
 * <p>Maps to the MuleSoft example's {@code request.Team}. The RAML required
 * id/name/homeCity; that contract is enforced here with Bean Validation.
 */
public class TeamRequest {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String homeCity;

    private String stadium;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
