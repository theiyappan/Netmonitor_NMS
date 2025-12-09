package org.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SystemInfo {
    private String name;
    private String description;
    private String uptime;
    private String contact;
    private String location;

    @JsonProperty("totalinterfaces")
    private String totalinterfaces;

    @JsonProperty("activeinterfaces")
    private String activeinterfaces;
}
