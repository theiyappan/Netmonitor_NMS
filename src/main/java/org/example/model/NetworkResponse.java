package org.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetworkResponse {

    private SystemInfo systemInfo;
    private List<InterfaceData> interfaces;
}
