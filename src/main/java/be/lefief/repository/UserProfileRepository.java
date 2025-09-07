package be.lefief.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserProfileRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public UserProfileRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate){
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public int save(UserProfile profile){
        return this.namedParameterJdbcTemplate.update(
                UserProfileHelper.INSERT_USERPROFILE,
                UserProfileHelper.INSERT_PARAMETERS(profile)
        );
    }

    public int update(UserData userData){
        return this.namedParameterJdbcTemplate.update(
                UserProfileHelper.UPDATE_USERPROFILE_BY_ID,
                UserProfileHelper.UPDATE_PARAMETERS(userData)
        );
    }

    public int delete(UUID id){
        return this.namedParameterJdbcTemplate.update(
                UserProfileHelper.DELETE_USERPROFILE_BY_ID,
                UserProfileHelper.BY_ID(id)
        );
    }

    public Optional<UserData> findByID(UUID id){
        return Optional.ofNullable(
                namedParameterJdbcTemplate.query(
                        UserProfileHelper.FIND_USERPROFILE_BY_ID,
                        UserProfileHelper.BY_ID(id),
                        UserProfileHelper.DATA_RESULTSET_EXTRACTOR
                )
        );
    }

    public Optional<UserData> findByName(String name){
        return Optional.ofNullable(
                namedParameterJdbcTemplate.query(
                        UserProfileHelper.FIND_USERPROFILE_BY_NAME,
                        UserProfileHelper.BY_NAME(name),
                        UserProfileHelper.DATA_RESULTSET_EXTRACTOR
                )
        );
    }

    public Optional<UserData> authenticate(String username, String password) {
        return Optional.ofNullable(
                namedParameterJdbcTemplate.query(
                        UserProfileHelper.LOGIN,
                        UserProfileHelper.BY_USERNAME_AND_PASSWORD(username, password),
                        UserProfileHelper.DATA_RESULTSET_EXTRACTOR
                )
        );
    }
}
