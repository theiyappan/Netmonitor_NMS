package org.example.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.example.model.NetworkSnap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.ArrayList;
import java.time.Instant;
import java.util.Optional;

@Repository
public class NetworkRepository {

    @Autowired
    private CqlSession cqlSession;

    @Autowired
    private ConversionService conversionService;

    public Optional<NetworkSnap> FindLatest(String systemName){
        // FIXED: Use correct table and column names, proper parameterization
        String query = "SELECT * FROM new_app.network_details WHERE system_name = ? LIMIT 1;";

        SimpleStatement stmt = SimpleStatement.builder(query)
                .addPositionalValue(systemName)
                .build();

        try {
            ResultSet rs = cqlSession.execute(stmt);
            Row row = rs.one();

            if(row != null){
                return Optional.of(convertoNetworkSnap(row));
            }
            return Optional.empty();

        } catch (Exception e) {
            System.err.println("Error in FindLatest: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<NetworkSnap> FindRecent(String systemName, int limit){
        String query = "SELECT * FROM new_app.network_details WHERE system_name = ? LIMIT ?";

        SimpleStatement stmt = SimpleStatement.builder(query)
                .addPositionalValue(systemName)
                .addPositionalValue(limit)  // FIXED: use the limit parameter
                .build();

        try {
            ResultSet rs = cqlSession.execute(stmt);

            List<NetworkSnap> snapShots = new ArrayList<>();
            for (Row row : rs) {
                snapShots.add(convertoNetworkSnap(row));
            }
            return snapShots;

        } catch (Exception e) {
            System.err.println("Error in FindRecent: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<String> findAllSystemNames() {
        String query = "SELECT DISTINCT system_name FROM new_app.network_details";

        try {
            ResultSet resultSet = cqlSession.execute(query);

            List<String> systemNames = new ArrayList<>();
            for (Row row : resultSet) {
                systemNames.add(row.getString("system_name"));
            }

            return systemNames;

        } catch (Exception e) {
            System.err.println("Error querying system names: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<NetworkSnap> FindbyTime(String systemName, Instant startTime, Instant endTime){
        // FIXED: Added keyspace prefix
        String query = "SELECT * FROM new_app.network_details " +
                "WHERE system_name = ? " +
                "AND snap_time >= ? " +
                "AND snap_time <= ?";

        SimpleStatement stmt = SimpleStatement.builder(query)
                .addPositionalValue(systemName)
                .addPositionalValue(startTime)
                .addPositionalValue(endTime)
                .build();

        try {
            ResultSet rs = cqlSession.execute(stmt);

            List<NetworkSnap> snapShots = new ArrayList<>();
            for (Row row : rs) {
                snapShots.add(convertoNetworkSnap(row));
            }
            return snapShots;

        } catch (Exception e) {
            System.err.println("Error in FindbyTime: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    private NetworkSnap convertoNetworkSnap(Row row){
        NetworkSnap snapshot = new NetworkSnap();

        snapshot.setSystemName(row.getString("system_name"));
        snapshot.setSnaptime(row.getInstant("snap_time"));
        snapshot.setSystemDesc(row.getString("system_desc"));
        snapshot.setUptime(row.getString("uptime"));
        snapshot.setTotalInterface(row.getInt("total_interface"));
        snapshot.setActiveInterface(row.getInt("active_interface"));
        snapshot.setInterfacejson(row.getString("interfaces_json"));

        return snapshot;
    }
}

