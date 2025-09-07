package be.lefief.repository;

import be.lefief.util.DateUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserProfileHelper {
    public static final String TABLE_NAME = "USERPROFILE";
    private static final String ID = "id";
    private static final String REGISTERED_AT = "registered_at";
    private static final String NAME = "name";
    private static final String PASSWORD = "password";
    private static String param(String column){return ":" + column;}
    public static final String INSERT_USERPROFILE =
            "INSERT INTO " + TABLE_NAME +
                    "(" + ID + ", " + REGISTERED_AT + ", " + NAME + ", " + PASSWORD + ") "
            + " VALUES " +
                    "(" + param(ID) + ", " + param(REGISTERED_AT) + ", " + param(NAME) + ", " + param(PASSWORD) + ");";
    public static MapSqlParameterSource INSERT_PARAMETERS(UserProfile userProfile){
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue(ID, userProfile.getId());
        mapSqlParameterSource.addValue(REGISTERED_AT, userProfile.getRegisteredAt());
        mapSqlParameterSource.addValue(NAME, userProfile.getName());
        mapSqlParameterSource.addValue(PASSWORD, userProfile.getPassword());
        return mapSqlParameterSource;
    }


    public static final String UPDATE_USERPROFILE_BY_ID =
            "UPDATE " + TABLE_NAME + " SET " + NAME + " = " + param(NAME) + " WHERE " + ID + " = " + param(ID);
    public static MapSqlParameterSource UPDATE_PARAMETERS(UserData userData){
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue(ID, userData.getId());
        mapSqlParameterSource.addValue(NAME, userData.getName());
        return mapSqlParameterSource;
    }

    public static final String DELETE_USERPROFILE_BY_ID = "DELETE FROM " + TABLE_NAME + " WHERE " + ID + " = " + param(ID);
    public static MapSqlParameterSource BY_ID(UUID id){
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue(ID, id);
        return mapSqlParameterSource;
    }
    public static final String FIND_USERPROFILE_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + ID + " = " + param(ID);

    public static ResultSetExtractor<UserProfile> RESULTSET_EXTRACTOR = new ResultSetExtractor<>() {
        @Override
        public UserProfile extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (!rs.next()) return null;
            return ROWMAPPER.mapRow(rs, 0);
        }
    };

    public static ResultSetExtractor<UserData> DATA_RESULTSET_EXTRACTOR = new ResultSetExtractor<>() {
        @Override
        public UserData extractData(ResultSet rs) throws SQLException, DataAccessException {
            if (!rs.next()) return null;
            return DATA_ROWMAPPER.mapRow(rs, 0);
        }
    };

    public static RowMapper<UserProfile> ROWMAPPER = (rs, rowNum) -> new UserProfile(
            Optional.ofNullable(rs.getString(ID)).map(UUID::fromString).orElse(null),
            rs.getString(NAME),
            DateUtil.toLocalDateTime(rs.getTimestamp(REGISTERED_AT)),
            rs.getString(PASSWORD)
    );
    public static RowMapper<UserData> DATA_ROWMAPPER = (rs, rowNum) -> new UserData(
            Optional.ofNullable(rs.getString(ID)).map(UUID::fromString).orElse(null),
            rs.getString(NAME),
            DateUtil.toLocalDateTime(rs.getTimestamp(REGISTERED_AT))
    );
    public static final String FIND_USERPROFILE_BY_NAME = "SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " LIKE " + param(NAME);

    public static MapSqlParameterSource BY_NAME(String name){
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue(NAME, name);
        return mapSqlParameterSource;
    }

    public static MapSqlParameterSource BY_USERNAME_AND_PASSWORD(String username, String password) {
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue(NAME, username);
        mapSqlParameterSource.addValue(PASSWORD, password);
        return mapSqlParameterSource;
    }
    public static final String LOGIN = "SELECT * FROM " + TABLE_NAME + " WHERE " + NAME + " = " + param(NAME) + " AND " + PASSWORD + " = " + param(PASSWORD);
}
