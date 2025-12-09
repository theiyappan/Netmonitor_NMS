package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.repository.NetworkRepository;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NetworkService {
    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public NetworkResponse getLatestNetwork(String SystemName) throws Exception{
        Optional<NetworkSnap> Snapper = networkRepository.FindLatest(SystemName);

        if(Snapper.isEmpty()){
            throw new Exception("Network Not Found");
        }
        NetworkSnap snap=Snapper.get();
        return parsetoResponse(snap.getInterfacejson());
    }

    public HistoricalResponse getHistoricalSnapshots(String systemName, int limit) throws Exception{
        List<NetworkSnap> Snapper= networkRepository.FindRecent(systemName,limit);

        if(Snapper.isEmpty()){
            throw new Exception("Network Not Found");
        }
        List<HistoricalResponse.SnapshotData>snapshotList=new ArrayList<>();

        for(NetworkSnap snap: Snapper){
            NetworkResponse networkResponse=parsetoResponse(snap.getInterfacejson());
            HistoricalResponse.SnapshotData snapshotData=new HistoricalResponse.SnapshotData(
                    snap.getSnaptime(),
                    networkResponse.getSystemInfo(),
                    networkResponse.getInterfaces()
            );
            snapshotList.add(snapshotData);
        }
        return new HistoricalResponse(
                systemName,
                snapshotList.size(),
                snapshotList);
    }

    public HistoricalResponse getSnapshotsByTimeRange(String systemName, Instant startTime, Instant endTime) throws Exception {
        List<NetworkSnap>Snapper=networkRepository.FindbyTime(systemName,startTime,endTime);

        if(Snapper.isEmpty()){
            throw new Exception("Network Not Found");
        }
        List<HistoricalResponse.SnapshotData>snapshotList=new ArrayList<>();

        for(NetworkSnap snap: Snapper){
            NetworkResponse networkResponse=parsetoResponse(snap.getInterfacejson());
            HistoricalResponse.SnapshotData snapshotData=new HistoricalResponse.SnapshotData(
                    snap.getSnaptime(),
                    networkResponse.getSystemInfo(),
                    networkResponse.getInterfaces()
            );
            snapshotList.add(snapshotData);
        }
        return new HistoricalResponse(
                systemName,
                snapshotList.size(),
                snapshotList);
    }

    private NetworkResponse parsetoResponse(String json) throws Exception{
        try{
            NetworkResponse neetworkResponse=objectMapper.readValue(json,NetworkResponse.class);
            return neetworkResponse;
        }
        catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }
    public java.util.Map<String, Object> getSnapshotSummary(String systemName) throws Exception {
        NetworkResponse response = getLatestNetwork(systemName);

        java.util.Map<String, Object> summary = new java.util.HashMap<>();

        long upInterfaces = response.getInterfaces().stream()
                .filter(i -> "up".equals(i.getStatus()))
                .count();
        long downInterfaces = response.getInterfaces().stream()
                .filter(i -> "down".equals(i.getStatus()))
                .count();

        summary.put("interfacesUp", upInterfaces);
        summary.put("interfacesDown", downInterfaces);

        // Calculate total traffic
        long totalRxBytes = response.getInterfaces().stream()
                .filter(i -> i.getTraffic() != null)
                .mapToLong(i -> i.getTraffic().getRxBytes() != null ? i.getTraffic().getRxBytes() : 0)
                .sum();

        long totalTxBytes = response.getInterfaces().stream()
                .filter(i -> i.getTraffic() != null)
                .mapToLong(i -> i.getTraffic().getTxBytes() != null ? i.getTraffic().getTxBytes() : 0)
                .sum();

        summary.put("totalRxBytes", totalRxBytes);
        summary.put("totalTxBytes", totalTxBytes);
        summary.put("totalBytes", totalRxBytes + totalTxBytes);

        return summary;
    }
    public List<String> getAllSystemNames() {
        return networkRepository.findAllSystemNames();
    }

}
